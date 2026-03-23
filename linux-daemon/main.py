from __future__ import annotations

import asyncio
import logging
import os
import subprocess
from dataclasses import dataclass
from typing import Awaitable, Callable

import websockets
from websockets.client import WebSocketClientProtocol

from handlers.clipboard import ClipboardPayload, write_clipboard
from handlers.file import IncomingFileTransfer, ensure_target_dir, notify_file_received, write_chunk
from handlers.screen import ScreenConfig, ScrcpyController
from handlers.sms import mirror_sms, prompt_sms_reply
from mdns_discover import discover_device
from protocol import Envelope, hello, hello_ack, ping, pong
from watchers.clipboard_watch import ClipboardWatcher

DEFAULT_SAVE_DIR = os.getenv("MINUX_SAVE_DIR", "~/Downloads/Monux")
FILE_TRANSFERS: dict[str, IncomingFileTransfer] = {}
TARGET_DIR = ensure_target_dir(DEFAULT_SAVE_DIR)
SCRCPY = ScrcpyController()

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")


@dataclass(slots=True)
class MonuxConfig:
    reconnect_delay: float = float(os.getenv("MINUX_RECONNECT_DELAY", "5"))
    heartbeat_interval: float = float(os.getenv("MINUX_HEARTBEAT_INTERVAL", "15"))
    discovery_timeout: float = float(os.getenv("MINUX_DISCOVERY_TIMEOUT", "5"))
    clipboard_poll_interval: float = float(os.getenv("MINUX_CLIPBOARD_POLL_INTERVAL", "1"))
    device_url: str = os.getenv("MINUX_DEVICE_URL", "")


class MessageDispatcher:
    def __init__(self) -> None:
        self._handlers: dict[str, Callable[[Envelope], Awaitable[None] | None]] = {
            "clipboard": self.handle_clipboard,
            "notify": self.handle_notify,
            "sms": self.handle_sms,
            "sms.sent": self.handle_sms_sent,
            "file.offer": self.handle_file_offer,
            "file.chunk": self.handle_file_chunk,
            "file.complete": self.handle_file_complete,
            "file.error": self.handle_file_error,
            "screen.start": self.handle_screen_start,
            "screen.stop": self.handle_screen_stop,
            "file": self.handle_file,
            "hello": self.handle_hello,
            "hello_ack": self.handle_hello_ack,
            "ping": self.handle_ping,
            "pong": self.handle_pong,
        }
        self._ws: WebSocketClientProtocol | None = None
        self._clipboard_watcher: ClipboardWatcher | None = None

    def bind(self, ws: WebSocketClientProtocol, clipboard_watcher: ClipboardWatcher) -> None:
        self._ws = ws
        self._clipboard_watcher = clipboard_watcher

    async def dispatch(self, message: Envelope) -> None:
        handler = self._handlers.get(message.type)
        if handler is None:
            logging.warning("unhandled message type=%s payload=%s", message.type, message.payload)
            return
        result = handler(message)
        if asyncio.iscoroutine(result):
            await result

    async def handle_clipboard(self, message: Envelope) -> None:
        text = str(message.payload.get("text", ""))
        content_hash = str(message.payload.get("content_hash", ""))
        if not text or not content_hash:
            return
        applied_hash = await asyncio.to_thread(write_clipboard, text)
        if applied_hash:
            if self._clipboard_watcher is not None:
                self._clipboard_watcher.remember_hash(applied_hash)
            logging.info("clipboard synced hash=%s", applied_hash[:12])

    async def handle_notify(self, message: Envelope) -> None:
        title = str(message.payload.get("title", "Phone"))
        body = str(message.payload.get("body", ""))
        result = subprocess.run(["notify-send", title, body], check=False)
        if result.returncode == 0:
            logging.info("notification mirrored title=%s", title)
        else:
            logging.warning("notify-send failed code=%s title=%s", result.returncode, title)

    async def handle_sms(self, message: Envelope) -> None:
        sender = str(message.payload.get("from", "SMS"))
        body = str(message.payload.get("body", ""))
        mirror_sms(sender, body)
        logging.info("sms mirrored from=%s", sender)
        reply = await asyncio.to_thread(prompt_sms_reply, sender, body)
        if reply and self._ws is not None:
            await self._ws.send(Envelope(type="sms.send", payload={"address": sender, "body": reply}).to_json())
            logging.info("sms reply sent to android sender=%s", sender)

    async def handle_sms_sent(self, message: Envelope) -> None:
        address = str(message.payload.get("address", ""))
        success = bool(message.payload.get("success", False))
        error = str(message.payload.get("error", ""))
        if success:
            logging.info("sms delivered address=%s", address)
        else:
            logging.warning("sms delivery failed address=%s error=%s", address, error)

    async def handle_file(self, message: Envelope) -> None:
        name = str(message.payload.get("name", "file"))
        logging.info("file event received name=%s", name)

    async def handle_file_offer(self, message: Envelope) -> None:
        transfer_id = str(message.payload.get("transfer_id", ""))
        name = str(message.payload.get("name", "shared-file"))
        if not transfer_id:
            return
        transfer = IncomingFileTransfer(transfer_id=transfer_id, name=name, target_dir=TARGET_DIR)
        if transfer.path.exists():
            transfer.path.unlink()
        FILE_TRANSFERS[transfer_id] = transfer
        logging.info("file offer accepted transfer_id=%s name=%s", transfer_id, name)

    async def handle_file_chunk(self, message: Envelope) -> None:
        transfer_id = str(message.payload.get("transfer_id", ""))
        data_base64 = str(message.payload.get("data_base64", ""))
        transfer = FILE_TRANSFERS.get(transfer_id)
        if transfer is None or not data_base64:
            return
        await asyncio.to_thread(write_chunk, transfer, data_base64)

    async def handle_file_complete(self, message: Envelope) -> None:
        transfer_id = str(message.payload.get("transfer_id", ""))
        transfer = FILE_TRANSFERS.pop(transfer_id, None)
        if transfer is None:
            return
        notify_file_received(transfer.path)
        logging.info("file received path=%s", transfer.path)
        if self._ws is not None:
            await self._ws.send(Envelope(type="file.received", payload={"transfer_id": transfer_id, "path": str(transfer.path)}).to_json())

    async def handle_file_error(self, message: Envelope) -> None:
        transfer_id = str(message.payload.get("transfer_id", ""))
        FILE_TRANSFERS.pop(transfer_id, None)
        logging.warning("file transfer error transfer_id=%s", transfer_id)

    async def handle_screen_start(self, message: Envelope) -> None:
        max_size = int(message.payload.get("max_size", 1600) or 1600)
        bitrate = str(message.payload.get("bitrate", "8M") or "8M")
        success, detail = await asyncio.to_thread(SCRCPY.start, ScreenConfig(max_size=max_size, bitrate=bitrate))
        logging.info("screen start success=%s detail=%s", success, detail)
        if self._ws is not None:
            await self._ws.send(Envelope(type="screen.started", payload={"success": success, "message": detail}).to_json())

    async def handle_screen_stop(self, message: Envelope) -> None:
        success, detail = await asyncio.to_thread(SCRCPY.stop)
        logging.info("screen stop success=%s detail=%s", success, detail)
        if self._ws is not None:
            await self._ws.send(Envelope(type="screen.started", payload={"success": False, "message": detail}).to_json())

    async def handle_hello(self, message: Envelope) -> None:
        logging.info(
            "handshake hello from device=%s platform=%s version=%s",
            message.payload.get("device_name", "unknown"),
            message.payload.get("platform", "unknown"),
            message.payload.get("version", "unknown"),
        )
        if self._ws is not None:
            await self._ws.send(hello_ack().to_json())

    async def handle_hello_ack(self, message: Envelope) -> None:
        logging.info(
            "handshake ack from device=%s platform=%s version=%s",
            message.payload.get("device_name", "unknown"),
            message.payload.get("platform", "unknown"),
            message.payload.get("version", "unknown"),
        )

    async def handle_ping(self, message: Envelope) -> None:
        if self._ws is not None:
            await self._ws.send(pong().to_json())
        logging.debug("ping received")

    async def handle_pong(self, message: Envelope) -> None:
        logging.debug("pong received")


class MonuxDaemon:
    def __init__(self, config: MonuxConfig) -> None:
        self.config = config
        self.dispatcher = MessageDispatcher()

    async def run(self) -> None:
        while True:
            try:
                await self._run_session()
            except Exception as exc:
                logging.exception("connection loop failed: %s", exc)
            await asyncio.sleep(self.config.reconnect_delay)

    async def _run_session(self) -> None:
        device_url = await self._resolve_device_url()
        logging.info("connecting to %s", device_url)
        async with websockets.connect(device_url, ping_interval=None, ping_timeout=None) as ws:
            clipboard_watcher = ClipboardWatcher(self._push_clipboard_update, self.config.clipboard_poll_interval)
            self.dispatcher.bind(ws, clipboard_watcher)
            heartbeat_task = asyncio.create_task(self._heartbeat_loop(ws))
            clipboard_task = asyncio.create_task(clipboard_watcher.run())
            try:
                await self._on_connect(ws)
                async for raw in ws:
                    await self._handle_message(raw)
            finally:
                heartbeat_task.cancel()
                clipboard_task.cancel()
                await asyncio.gather(heartbeat_task, clipboard_task, return_exceptions=True)

    async def _resolve_device_url(self) -> str:
        if self.config.device_url:
            return self.config.device_url
        device = await asyncio.to_thread(discover_device, self.config.discovery_timeout)
        if device is None:
            raise RuntimeError("no Monux Android device discovered via mDNS")
        logging.info("discovered android device name=%s host=%s port=%s", device.name, device.host, device.port)
        return device.ws_url

    async def _on_connect(self, ws: WebSocketClientProtocol) -> None:
        await ws.send(hello().to_json())
        logging.info("hello sent")

    async def _heartbeat_loop(self, ws: WebSocketClientProtocol) -> None:
        while True:
            await asyncio.sleep(self.config.heartbeat_interval)
            await ws.send(ping().to_json())
            logging.debug("ping sent")

    async def _push_clipboard_update(self, payload: ClipboardPayload) -> None:
        if self.dispatcher._ws is None:
            return
        envelope = Envelope(
            type="clipboard",
            payload={"text": payload.text, "content_hash": payload.content_hash},
        )
        await self.dispatcher._ws.send(envelope.to_json())
        logging.info("clipboard pushed hash=%s", payload.content_hash[:12])

    async def _handle_message(self, raw: str) -> None:
        try:
            message = Envelope.from_json(raw)
        except Exception:
            logging.warning("invalid json: %s", raw)
            return
        await self.dispatcher.dispatch(message)


def main() -> None:
    config = MonuxConfig()
    daemon = MonuxDaemon(config)
    asyncio.run(daemon.run())


if __name__ == "__main__":
    main()
