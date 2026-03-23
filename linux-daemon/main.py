from __future__ import annotations

import asyncio
import json
import logging
import os
import subprocess
from dataclasses import dataclass
from typing import Any, Callable

import websockets
from websockets.client import WebSocketClientProtocol

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")


@dataclass(slots=True)
class MinuxConfig:
    device_url: str = os.getenv("MINUX_DEVICE_URL", "ws://minux.local:8765/ws")
    reconnect_delay: float = float(os.getenv("MINUX_RECONNECT_DELAY", "3"))


class MessageDispatcher:
    def __init__(self) -> None:
        self._handlers: dict[str, Callable[[dict[str, Any]], asyncio.Future[Any] | Any]] = {
            "clipboard": self.handle_clipboard,
            "notify": self.handle_notify,
            "sms": self.handle_sms,
            "file": self.handle_file,
        }

    async def dispatch(self, message: dict[str, Any]) -> None:
        kind = str(message.get("type", ""))
        handler = self._handlers.get(kind)
        if handler is None:
            logging.warning("unhandled message type=%s payload=%s", kind, message)
            return
        result = handler(message)
        if asyncio.iscoroutine(result):
            await result

    async def handle_clipboard(self, message: dict[str, Any]) -> None:
        text = str(message.get("text", ""))
        if not text:
            return
        subprocess.run(["xclip", "-selection", "clipboard"], input=text.encode(), check=False)
        logging.info("clipboard synced")

    async def handle_notify(self, message: dict[str, Any]) -> None:
        title = str(message.get("title", "Phone"))
        body = str(message.get("body", ""))
        subprocess.run(["notify-send", title, body], check=False)
        logging.info("notification mirrored")

    async def handle_sms(self, message: dict[str, Any]) -> None:
        sender = str(message.get("from", "SMS"))
        body = str(message.get("body", ""))
        subprocess.run(["notify-send", f"SMS from {sender}", body], check=False)
        logging.info("sms mirrored")

    async def handle_file(self, message: dict[str, Any]) -> None:
        name = str(message.get("name", "file"))
        logging.info("file event received name=%s", name)


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
        logging.info("connecting to %s", self.config.device_url)
        async with websockets.connect(self.config.device_url, ping_interval=20, ping_timeout=20) as ws:
            await self._on_connect(ws)
            async for raw in ws:
                await self._handle_message(raw)

    async def _on_connect(self, ws: WebSocketClientProtocol) -> None:
        hello = {"type": "hello", "role": "linux-daemon", "capabilities": ["notify", "clipboard", "sms", "file", "screen"]}
        await ws.send(json.dumps(hello))
        logging.info("connected")

    async def _handle_message(self, raw: str) -> None:
        try:
            message = json.loads(raw)
        except json.JSONDecodeError:
            logging.warning("invalid json: %s", raw)
            return
        await self.dispatcher.dispatch(message)


def main() -> None:
    config = MinuxConfig()
    daemon = MinuxDaemon(config)
    asyncio.run(daemon.run())


if __name__ == "__main__":
    main()
