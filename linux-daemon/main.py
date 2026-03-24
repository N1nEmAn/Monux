from __future__ import annotations

import asyncio
import logging
import os
import subprocess
from dataclasses import dataclass, field
from datetime import datetime
from typing import Awaitable, Callable

import websockets
from rich import box
from rich.console import Console, Group
from rich.live import Live
from rich.panel import Panel
from rich.table import Table
from rich.text import Text
from websockets.client import WebSocketClientProtocol
from websockets.exceptions import ConnectionClosed

from handlers.clipboard import ClipboardPayload, write_clipboard
from handlers.file import IncomingFileTransfer, ensure_target_dir, notify_file_received, write_chunk
from handlers.input import press_key, type_text
from handlers.screen import ScreenConfig, ScrcpyController
from handlers.sms import mirror_sms, prompt_sms_reply
from mdns_discover import discover_device
from protocol import Envelope, hello, hello_ack, ping, pong
from watchers.clipboard_watch import ClipboardWatcher

DEFAULT_SAVE_DIR = os.getenv("MINUX_SAVE_DIR", "~/Downloads/Monux")
FILE_TRANSFERS: dict[str, IncomingFileTransfer] = {}
TARGET_DIR = ensure_target_dir(DEFAULT_SAVE_DIR)
SCRCPY = ScrcpyController()
CONSOLE = Console()

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")


@dataclass(slots=True)
class MonuxConfig:
    reconnect_delay: float = float(os.getenv("MINUX_RECONNECT_DELAY", "5"))
    heartbeat_interval: float = float(os.getenv("MINUX_HEARTBEAT_INTERVAL", "15"))
    discovery_timeout: float = float(os.getenv("MINUX_DISCOVERY_TIMEOUT", "5"))
    clipboard_poll_interval: float = float(os.getenv("MINUX_CLIPBOARD_POLL_INTERVAL", "1"))
    device_url: str = os.getenv("MINUX_DEVICE_URL", "")


@dataclass(slots=True)
class DashboardState:
    status: str = "启动中"
    device_name: str = "未发现"
    device_url: str = "等待发现"
    processed_messages: int = 0
    last_event: str = "初始化完成"
    connected_since: str = "-"
    last_updated: str = field(default_factory=lambda: datetime.now().strftime("%H:%M:%S"))

    def touch(self, event: str) -> None:
        self.last_event = event
        self.last_updated = datetime.now().strftime("%H:%M:%S")


class MonuxDashboard:
    def __init__(self) -> None:
        self.state = DashboardState()

    def update(self, **kwargs: str | int) -> None:
        for key, value in kwargs.items():
            setattr(self.state, key, value)
        if "last_event" in kwargs:
            self.state.last_updated = datetime.now().strftime("%H:%M:%S")

    def record_message(self, message_type: str) -> None:
        self.state.processed_messages += 1
        self.state.touch(f"收到消息：{message_type}")

    def render(self) -> Panel:
        banner = Text(
            """
███╗   ███╗ ██████╗ ███╗   ██╗██╗   ██╗██╗  ██╗
████╗ ████║██╔═══██╗████╗  ██║██║   ██║╚██╗██╔╝
██╔████╔██║██║   ██║██╔██╗ ██║██║   ██║ ╚███╔╝
██║╚██╔╝██║██║   ██║██║╚██╗██║██║   ██║ ██╔██╗
██║ ╚═╝ ██║╚██████╔╝██║ ╚████║╚██████╔╝██╔╝ ██╗
╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝ ╚═╝  ╚═╝
            """.strip("\n"),
            style="bold #FF8A3D",
        )

        table = Table.grid(padding=(0, 2))
        table.add_column(style="bold #FFB36C", justify="right")
        table.add_column(style="#FFF1E6")
        table.add_row("连接状态", self.state.status)
        table.add_row("设备名", self.state.device_name)
        table.add_row("目标地址", self.state.device_url)
        table.add_row("已处理消息", str(self.state.processed_messages))
        table.add_row("连接开始", self.state.connected_since)
        table.add_row("最近事件", self.state.last_event)
        table.add_row("刷新时间", self.state.last_updated)

        content = Group(
            banner,
            Text("\nMonux daemon live dashboard", style="#FFB36C"),
            Panel(table, box=box.ROUNDED, border_style="#FF8A3D", title="实时状态", padding=(1, 1)),
        )
        return Panel(content, box=box.HEAVY, border_style="#FF6A00", title="Monux", subtitle="rich.live")


class SessionDisconnected(RuntimeError):
    def __init__(self, reason: str) -> None:
        super().__init__(reason)
        self.reason = reason


class MessageDispatcher:
    def __init__(self, dashboard: MonuxDashboard | None = None) -> None:
        self.dashboard = dashboard or MonuxDashboard()
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
            "input.text": self.handle_input_text,
            "input.key": self.handle_input_key,
            "input.voice": self.handle_input_text,
            "file": self.handle_file,
            "hello": self.handle_hello,
            "hello_ack": self.handle_hello_ack,
            "ping": self.handle_ping,
            "pong": self.handle_pong,
        }
        self._ws: WebSocketClientProtocol | None = None
        self._clipboard_watcher: ClipboardWatcher | None = None
        self._send_json: Callable[[str, str], Awaitable[bool]] | None = None

    def bind(
        self,
        ws: WebSocketClientProtocol,
        clipboard_watcher: ClipboardWatcher,
        send_json: Callable[[str, str], Awaitable[bool]],
    ) -> None:
        self._ws = ws
        self._clipboard_watcher = clipboard_watcher
        self._send_json = send_json

    async def dispatch(self, message: Envelope) -> None:
        self.dashboard.record_message(message.type)
        handler = self._handlers.get(message.type)
        if handler is None:
            logging.warning("unhandled message type=%s payload=%s", message.type, message.payload)
            self.dashboard.update(last_event=f"未处理消息：{message.type}")
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
            self.dashboard.update(last_event=f"剪贴板已同步 {applied_hash[:8]}")

    async def handle_notify(self, message: Envelope) -> None:
        title = str(message.payload.get("title", "Phone"))
        body = str(message.payload.get("body", ""))
        result = subprocess.run(["notify-send", title, body], check=False)
        if result.returncode == 0:
            logging.info("notification mirrored title=%s", title)
            self.dashboard.update(last_event=f"通知镜像：{title}")
        else:
            logging.warning("notify-send failed code=%s title=%s", result.returncode, title)
            self.dashboard.update(last_event=f"通知发送失败：{title}")

    async def handle_sms(self, message: Envelope) -> None:
        sender = str(message.payload.get("from", "SMS"))
        body = str(message.payload.get("body", ""))
        mirror_sms(sender, body)
        logging.info("sms mirrored from=%s", sender)
        self.dashboard.update(last_event=f"短信来自：{sender}")
        reply = await asyncio.to_thread(prompt_sms_reply, sender, body)
        if reply and await self._send(Envelope(type="sms.send", payload={"address": sender, "body": reply}).to_json(), "sms.send reply"):
            logging.info("sms reply sent to android sender=%s", sender)
            self.dashboard.update(last_event=f"短信已回复：{sender}")

    async def handle_sms_sent(self, message: Envelope) -> None:
        address = str(message.payload.get("address", ""))
        success = bool(message.payload.get("success", False))
        error = str(message.payload.get("error", ""))
        if success:
            logging.info("sms delivered address=%s", address)
            self.dashboard.update(last_event=f"短信发送成功：{address}")
        else:
            logging.warning("sms delivery failed address=%s error=%s", address, error)
            self.dashboard.update(last_event=f"短信发送失败：{address}")

    async def handle_file(self, message: Envelope) -> None:
        name = str(message.payload.get("name", "file"))
        logging.info("file event received name=%s", name)
        self.dashboard.update(last_event=f"文件事件：{name}")

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
        self.dashboard.update(last_event=f"接收文件：{name}")

    async def handle_file_chunk(self, message: Envelope) -> None:
        transfer_id = str(message.payload.get("transfer_id", ""))
        data_base64 = str(message.payload.get("data_base64", ""))
        transfer = FILE_TRANSFERS.get(transfer_id)
        if transfer is None or not data_base64:
            return
        await asyncio.to_thread(write_chunk, transfer, data_base64)
        self.dashboard.update(last_event=f"文件传输中：{transfer.name}")

    async def handle_file_complete(self, message: Envelope) -> None:
        transfer_id = str(message.payload.get("transfer_id", ""))
        transfer = FILE_TRANSFERS.pop(transfer_id, None)
        if transfer is None:
            return
        notify_file_received(transfer.path)
        logging.info("file received path=%s", transfer.path)
        self.dashboard.update(last_event=f"文件完成：{transfer.name}")
        await self._send(
            Envelope(type="file.received", payload={"transfer_id": transfer_id, "path": str(transfer.path)}).to_json(),
            "file.received ack",
        )

    async def handle_file_error(self, message: Envelope) -> None:
        transfer_id = str(message.payload.get("transfer_id", ""))
        FILE_TRANSFERS.pop(transfer_id, None)
        logging.warning("file transfer error transfer_id=%s", transfer_id)
        self.dashboard.update(last_event=f"文件传输错误：{transfer_id[:8]}")

    async def handle_screen_start(self, message: Envelope) -> None:
        max_size = int(message.payload.get("max_size", 1600) or 1600)
        bitrate = str(message.payload.get("bitrate", "8M") or "8M")
        success, detail = await asyncio.to_thread(SCRCPY.start, ScreenConfig(max_size=max_size, bitrate=bitrate))
        logging.info("screen start success=%s detail=%s", success, detail)
        self.dashboard.update(last_event=f"投屏启动：{detail}")
        await self._send(Envelope(type="screen.started", payload={"success": success, "message": detail}).to_json(), "screen.started")

    async def handle_screen_stop(self, message: Envelope) -> None:
        success, detail = await asyncio.to_thread(SCRCPY.stop)
        logging.info("screen stop success=%s detail=%s", success, detail)
        self.dashboard.update(last_event=f"投屏停止：{detail}")
        await self._send(Envelope(type="screen.started", payload={"success": False, "message": detail}).to_json(), "screen.stopped")

    async def handle_input_text(self, message: Envelope) -> None:
        text = str(message.payload.get("text", ""))
        if not text:
            return
        success, detail = await asyncio.to_thread(type_text, text)
        logging.info("input text success=%s detail=%s", success, detail)
        snippet = text.replace("\n", " ")[:24]
        if success:
            self.dashboard.update(last_event=f"远程输入：{snippet}")
        else:
            self.dashboard.update(last_event=f"输入失败：{detail}")

    async def handle_input_key(self, message: Envelope) -> None:
        key = str(message.payload.get("key", ""))
        if not key:
            return
        success, detail = await asyncio.to_thread(press_key, key)
        logging.info("input key success=%s key=%s detail=%s", success, key, detail)
        if success:
            self.dashboard.update(last_event=f"远程按键：{key}")
        else:
            self.dashboard.update(last_event=f"按键失败：{detail}")

    async def handle_hello(self, message: Envelope) -> None:
        device_name = str(message.payload.get("device_name", "unknown"))
        logging.info(
            "handshake hello from device=%s platform=%s version=%s",
            device_name,
            message.payload.get("platform", "unknown"),
            message.payload.get("version", "unknown"),
        )
        self.dashboard.update(device_name=device_name, last_event=f"收到 hello：{device_name}")
        await self._send(hello_ack().to_json(), "hello_ack")

    async def handle_hello_ack(self, message: Envelope) -> None:
        device_name = str(message.payload.get("device_name", "unknown"))
        logging.info(
            "handshake ack from device=%s platform=%s version=%s",
            device_name,
            message.payload.get("platform", "unknown"),
            message.payload.get("version", "unknown"),
        )
        self.dashboard.update(device_name=device_name, last_event=f"握手完成：{device_name}")

    async def handle_ping(self, message: Envelope) -> None:
        await self._send(pong().to_json(), "pong")
        logging.debug("ping received")

    async def handle_pong(self, message: Envelope) -> None:
        logging.debug("pong received")

    async def _send(self, payload: str, context: str) -> bool:
        sender = self._send_json
        if sender is None or self._ws is None:
            return False
        return await sender(payload, context)


class MonuxDaemon:
    def __init__(self, config: MonuxConfig) -> None:
        self.config = config
        self.dashboard = MonuxDashboard()
        self.dispatcher = MessageDispatcher(self.dashboard)
        self._session_closing = False
        self._disconnect_reason = "连接已断开"
        self._active_ws: WebSocketClientProtocol | None = None

    async def run(self) -> None:
        with Live(self.dashboard.render(), console=CONSOLE, refresh_per_second=4, screen=False) as live:
            while True:
                try:
                    live.update(self.dashboard.render())
                    await self._run_session(live)
                except SessionDisconnected as exc:
                    logging.info("session ended: %s", exc.reason)
                    self.dashboard.update(status="等待重连", last_event=exc.reason)
                    live.update(self.dashboard.render())
                except Exception as exc:
                    logging.exception("connection loop failed: %s", exc)
                    self.dashboard.update(status="连接异常", last_event=str(exc))
                    live.update(self.dashboard.render())
                await asyncio.sleep(self.config.reconnect_delay)

    async def _run_session(self, live: Live) -> None:
        self.dashboard.update(status="正在发现设备", last_event="mDNS 搜索中")
        live.update(self.dashboard.render())
        device_url = await self._resolve_device_url()
        self.dashboard.update(device_url=device_url, status="正在连接", last_event=f"连接 {device_url}")
        live.update(self.dashboard.render())
        logging.info("connecting to %s", device_url)
        self._session_closing = False
        self._disconnect_reason = "连接已断开"
        async with websockets.connect(device_url, ping_interval=None, ping_timeout=None) as ws:
            self._active_ws = ws
            clipboard_watcher = ClipboardWatcher(self._push_clipboard_update, self.config.clipboard_poll_interval)
            self.dispatcher.bind(ws, clipboard_watcher, self._safe_send_text)
            heartbeat_task = asyncio.create_task(self._heartbeat_loop(ws, live))
            clipboard_task = asyncio.create_task(clipboard_watcher.run())
            self.dashboard.update(status="已连接", connected_since=datetime.now().strftime("%H:%M:%S"), last_event="WebSocket 已连接")
            live.update(self.dashboard.render())
            try:
                await self._on_connect(ws, live)
                try:
                    async for raw in ws:
                        await self._handle_message(raw, live)
                    close_code = getattr(ws, "close_code", None)
                    close_reason = getattr(ws, "close_reason", None) or "远端结束会话"
                    if close_code in (1000, 1001):
                        self._disconnect_reason = f"连接正常关闭 code={close_code} reason={close_reason}"
                    elif close_code is None:
                        self._disconnect_reason = close_reason
                    else:
                        self._disconnect_reason = f"连接中断 code={close_code} reason={close_reason}"
                    raise SessionDisconnected(self._disconnect_reason)
                except ConnectionClosed as exc:
                    reason = self._classify_disconnect(exc)
                    self._disconnect_reason = reason
                    self.dashboard.update(status="等待重连", last_event=reason)
                    live.update(self.dashboard.render())
                    raise SessionDisconnected(reason) from exc
            finally:
                self._session_closing = True
                heartbeat_task.cancel()
                clipboard_task.cancel()
                await asyncio.gather(heartbeat_task, clipboard_task, return_exceptions=True)
                try:
                    if not ws.closed:
                        await ws.close(code=1000, reason="daemon session closing")
                    await asyncio.wait_for(ws.wait_closed(), timeout=1)
                except Exception as exc:
                    logging.debug("graceful close skipped: %s", exc)
                self._active_ws = None
                self.dashboard.update(status="等待重连", last_event=self._disconnect_reason)
                live.update(self.dashboard.render())

    async def _resolve_device_url(self) -> str:
        if self.config.device_url:
            return self.config.device_url
        device = await asyncio.to_thread(discover_device, self.config.discovery_timeout)
        if device is None:
            raise RuntimeError("no Monux Android device discovered via mDNS")
        logging.info("discovered android device name=%s host=%s port=%s", device.name, device.host, device.port)
        self.dashboard.update(device_name=device.name)
        return device.ws_url

    async def _on_connect(self, ws: WebSocketClientProtocol, live: Live) -> None:
        await self._safe_send_text(hello().to_json(), "hello")
        logging.info("hello sent")
        self.dashboard.update(last_event="已发送 hello")
        live.update(self.dashboard.render())

    async def _heartbeat_loop(self, ws: WebSocketClientProtocol, live: Live) -> None:
        while True:
            await asyncio.sleep(self.config.heartbeat_interval)
            if ws is not self._active_ws or self._session_closing:
                return
            await self._safe_send_text(ping().to_json(), "heartbeat ping")
            logging.debug("ping sent")
            self.dashboard.update(last_event="心跳 ping 已发送")
            live.update(self.dashboard.render())

    async def _push_clipboard_update(self, payload: ClipboardPayload) -> None:
        if self.dispatcher._ws is None or self._session_closing:
            return
        envelope = Envelope(
            type="clipboard",
            payload={"text": payload.text, "content_hash": payload.content_hash},
        )
        await self._safe_send_text(envelope.to_json(), "clipboard sync")
        logging.info("clipboard pushed hash=%s", payload.content_hash[:12])
        self.dashboard.update(last_event=f"推送剪贴板：{payload.content_hash[:8]}")

    async def _safe_send_text(self, payload: str, context: str) -> bool:
        ws = self._active_ws
        if ws is None or self._session_closing:
            return False
        try:
            await ws.send(payload)
            return True
        except ConnectionClosed as exc:
            reason = self._classify_disconnect(exc)
            self._disconnect_reason = reason
            logging.info("send interrupted context=%s reason=%s", context, reason)
            raise SessionDisconnected(reason) from exc
        except Exception as exc:
            reason = f"{context} 发送失败: {exc}"
            self._disconnect_reason = reason
            logging.warning("send failed context=%s", context, exc_info=exc)
            raise SessionDisconnected(reason) from exc

    def _classify_disconnect(self, exc: ConnectionClosed) -> str:
        code = getattr(exc, "code", None)
        reason = getattr(exc, "reason", None) or "peer closed"
        if code in (1000, 1001):
            return f"连接正常关闭 code={code} reason={reason}"
        if "no close frame received or sent" in str(exc):
            return "连接异常中断：未完成 close 握手"
        return f"连接中断 code={code} reason={reason}"

    async def _handle_message(self, raw: str, live: Live) -> None:
        try:
            message = Envelope.from_json(raw)
        except Exception:
            logging.warning("invalid json: %s", raw)
            self.dashboard.update(last_event="收到无效 JSON")
            live.update(self.dashboard.render())
            return
        await self.dispatcher.dispatch(message)
        live.update(self.dashboard.render())


def main() -> None:
    config = MonuxConfig()
    daemon = MonuxDaemon(config)
    asyncio.run(daemon.run())


if __name__ == "__main__":
    main()
