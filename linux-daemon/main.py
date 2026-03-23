from __future__ import annotations

import asyncio
import logging
import os
import subprocess
from dataclasses import dataclass
from typing import Awaitable, Callable

import websockets
from websockets.client import WebSocketClientProtocol

from mdns_discover import discover_device
from protocol import Envelope, hello, hello_ack, ping, pong

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")


@dataclass(slots=True)
class MinuxConfig:
    reconnect_delay: float = float(os.getenv("MINUX_RECONNECT_DELAY", "5"))
    heartbeat_interval: float = float(os.getenv("MINUX_HEARTBEAT_INTERVAL", "15"))
    discovery_timeout: float = float(os.getenv("MINUX_DISCOVERY_TIMEOUT", "5"))
    device_url: str = os.getenv("MINUX_DEVICE_URL", "")


class MessageDispatcher:
    def __init__(self) -> None:
        self._handlers: dict[str, Callable[[Envelope], Awaitable[None] | None]] = {
            "clipboard": self.handle_clipboard,
            "notify": self.handle_notify,
            "sms": self.handle_sms,
            "file": self.handle_file,
            "hello": self.handle_hello,
            "hello_ack": self.handle_hello_ack,
            "ping": self.handle_ping,
            "pong": self.handle_pong,
        }
        self._ws: WebSocketClientProtocol | None = None

    def bind(self, ws: WebSocketClientProtocol) -> None:
        self._ws = ws

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
        if not text:
            return
        subprocess.run(["xclip", "-selection", "clipboard"], input=text.encode(), check=False)
        logging.info("clipboard synced")

    async def handle_notify(self, message: Envelope) -> None:
        title = str(message.payload.get("title", "Phone"))
        body = str(message.payload.get("body", ""))
        subprocess.run(["notify-send", title, body], check=False)
        logging.info("notification mirrored")

    async def handle_sms(self, message: Envelope) -> None:
        sender = str(message.payload.get("from", "SMS"))
        body = str(message.payload.get("body", ""))
        subprocess.run(["notify-send", f"SMS from {sender}", body], check=False)
        logging.info("sms mirrored")

    async def handle_file(self, message: Envelope) -> None:
        name = str(message.payload.get("name", "file"))
        logging.info("file event received name=%s", name)

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


class MinuxDaemon:
    def __init__(self, config: MinuxConfig) -> None:
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
            self.dispatcher.bind(ws)
            heartbeat_task = asyncio.create_task(self._heartbeat_loop(ws))
            try:
                await self._on_connect(ws)
                async for raw in ws:
                    await self._handle_message(raw)
            finally:
                heartbeat_task.cancel()
                await asyncio.gather(heartbeat_task, return_exceptions=True)

    async def _resolve_device_url(self) -> str:
        if self.config.device_url:
            return self.config.device_url
        device = await asyncio.to_thread(discover_device, self.config.discovery_timeout)
        if device is None:
            raise RuntimeError("no Minux Android device discovered via mDNS")
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

    async def _handle_message(self, raw: str) -> None:
        try:
            message = Envelope.from_json(raw)
        except Exception:
            logging.warning("invalid json: %s", raw)
            return
        await self.dispatcher.dispatch(message)


def main() -> None:
    config = MinuxConfig()
    daemon = MinuxDaemon(config)
    asyncio.run(daemon.run())


if __name__ == "__main__":
    main()
