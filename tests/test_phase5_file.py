from __future__ import annotations

import asyncio
import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LINUX_DAEMON = ROOT / "linux-daemon"
sys.path.insert(0, str(LINUX_DAEMON))

main_spec = importlib.util.spec_from_file_location("monux_main", LINUX_DAEMON / "main.py")
main = importlib.util.module_from_spec(main_spec)
assert main_spec and main_spec.loader
sys.modules["monux_main"] = main  # required for @dataclass(slots=True) on Python 3.14
main_spec.loader.exec_module(main)

from handlers.file import IncomingFileTransfer, ensure_target_dir, write_chunk


class StubSocket:
    def __init__(self) -> None:
        self.messages: list[str] = []

    async def send(self, message: str) -> None:
        self.messages.append(message)


def test_ensure_target_dir_creates_directory(tmp_path: Path) -> None:
    target = ensure_target_dir(str(tmp_path / "downloads"))
    assert target.exists()
    assert target.is_dir()


def test_write_chunk_appends_file_content(tmp_path: Path) -> None:
    transfer = IncomingFileTransfer("t1", "sample.txt", tmp_path)
    write_chunk(transfer, "aGVsbG8=")
    write_chunk(transfer, "IHdvcmxk")
    assert transfer.path.read_text() == "hello world"


def test_file_offer_chunk_complete_flow(monkeypatch, tmp_path: Path) -> None:
    dispatcher = main.MessageDispatcher()
    dispatcher._ws = StubSocket()
    monkeypatch.setattr(main, "TARGET_DIR", tmp_path)
    monkeypatch.setattr(main, "FILE_TRANSFERS", {})

    notified: list[Path] = []
    monkeypatch.setattr(main, "notify_file_received", lambda path: notified.append(path))

    async def scenario() -> None:
        await dispatcher.handle_file_offer(main.Envelope("file.offer", {
            "transfer_id": "abc",
            "name": "demo.txt",
            "size": 11,
        }))
        assert "abc" in main.FILE_TRANSFERS

        await dispatcher.handle_file_chunk(main.Envelope("file.chunk", {
            "transfer_id": "abc",
            "index": 0,
            "total": 1,
            "data_base64": "aGVsbG8gd29ybGQ=",
        }))

        await dispatcher.handle_file_complete(main.Envelope("file.complete", {
            "transfer_id": "abc",
            "name": "demo.txt",
        }))

    asyncio.run(scenario())

    saved = tmp_path / "demo.txt"
    assert saved.read_text() == "hello world"
    assert notified == [saved]
    assert main.FILE_TRANSFERS == {}
    assert dispatcher._ws.messages
    assert 'file.received' in dispatcher._ws.messages[0]


def test_file_error_removes_transfer(monkeypatch, tmp_path: Path) -> None:
    dispatcher = main.MessageDispatcher()
    monkeypatch.setattr(main, "FILE_TRANSFERS", {
        "oops": IncomingFileTransfer("oops", "bad.bin", tmp_path)
    })

    async def scenario() -> None:
        await dispatcher.handle_file_error(main.Envelope("file.error", {"transfer_id": "oops"}))

    asyncio.run(scenario())
    assert "oops" not in main.FILE_TRANSFERS
