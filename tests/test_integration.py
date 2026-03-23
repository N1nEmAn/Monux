from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / 'linux-daemon' / 'main.py').read_text(encoding='utf-8')
PROTOCOL = (ROOT / 'linux-daemon' / 'protocol.py').read_text(encoding='utf-8')
MOCK = (ROOT / 'tests' / 'mock_android.py').read_text(encoding='utf-8')


def test_protocol_defines_hello_ping_pong() -> None:
    assert 'TYPE_HELLO = "hello"' in PROTOCOL
    assert 'TYPE_PING = "ping"' in PROTOCOL
    assert 'TYPE_PONG = "pong"' in PROTOCOL


def test_daemon_sends_hello_and_handles_ping() -> None:
    assert 'await ws.send(hello().to_json())' in MAIN
    assert 'await self._ws.send(pong().to_json())' in MAIN


def test_mock_android_covers_handshake_and_file_flow() -> None:
    assert 'hello_ack' in MOCK
    assert 'file.offer' in MOCK
    assert 'file.chunk' in MOCK
    assert 'file.complete' in MOCK
