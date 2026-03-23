from __future__ import annotations

import asyncio
from collections.abc import Awaitable, Callable

from handlers.clipboard import ClipboardPayload, read_clipboard


class ClipboardWatcher:
    def __init__(self, on_change: Callable[[ClipboardPayload], Awaitable[None]], interval: float = 1.0) -> None:
        self._on_change = on_change
        self._interval = interval
        self._last_hash = ""

    def remember_hash(self, content_hash: str) -> None:
        self._last_hash = content_hash

    async def run(self) -> None:
        while True:
            payload = await asyncio.to_thread(read_clipboard)
            if payload and payload.content_hash and payload.content_hash != self._last_hash:
                self._last_hash = payload.content_hash
                await self._on_change(payload)
            await asyncio.sleep(self._interval)
