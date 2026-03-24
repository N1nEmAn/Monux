from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = (ROOT / 'linux-daemon' / 'main.py').read_text(encoding='utf-8')
PROTOCOL = (ROOT / 'linux-daemon' / 'protocol.py').read_text(encoding='utf-8')
MOCK = (ROOT / 'tests' / 'mock_android.py').read_text(encoding='utf-8')
ANDROID_SERVER = (ROOT / 'android-app' / 'app' / 'src' / 'main' / 'kotlin' / 'com' / 'monux' / 'network' / 'WebSocketServer.kt').read_text(encoding='utf-8')
ANDROID_SERVICE = (ROOT / 'android-app' / 'app' / 'src' / 'main' / 'kotlin' / 'com' / 'monux' / 'MainService.kt').read_text(encoding='utf-8')


def test_protocol_defines_hello_ping_pong() -> None:
    assert 'TYPE_HELLO = "hello"' in PROTOCOL
    assert 'TYPE_PING = "ping"' in PROTOCOL
    assert 'TYPE_PONG = "pong"' in PROTOCOL


def test_daemon_uses_safe_send_for_handshake_and_ping() -> None:
    assert 'await self._safe_send_text(hello().to_json(), "hello")' in MAIN
    assert 'await self._send(pong().to_json(), "pong")' in MAIN
    assert 'class SessionDisconnected(RuntimeError):' in MAIN
    assert '连接异常中断：未完成 close 握手' in MAIN


def test_android_server_cleans_up_dead_sockets() -> None:
    assert 'fun broadcast(text: String): Int {' in ANDROID_SERVER
    assert 'private val cleanedUp = AtomicBoolean(false)' in ANDROID_SERVER
    assert 'fun shutdown(reason: String)' in ANDROID_SERVER
    assert 'private fun cleanupSocket(reason: String?, exception: Throwable? = null)' in ANDROID_SERVER
    assert 'cleanupSocket(exception?.message ?: "socket error", exception)' in ANDROID_SERVER


def test_android_service_routes_outgoing_messages_through_broadcast_helper() -> None:
    assert 'private fun broadcastToLinux(payload: String, context: String): Boolean {' in ANDROID_SERVICE
    assert 'broadcastToLinux(Protocol.ping().toString(), "heartbeat ping")' in ANDROID_SERVICE
    assert 'sendMessage = { payload -> broadcastToLinux(payload, "clipboard") }' in ANDROID_SERVICE
    assert 'broadcastToLinux(ack.toString(), "sms.sent ack")' in ANDROID_SERVICE
    assert 'return service.broadcastToLinux(payload.toString(), "remote input text")' in ANDROID_SERVICE


def test_mock_android_covers_handshake_file_flow_and_abrupt_close() -> None:
    assert 'hello_ack' in MOCK
    assert 'file.offer' in MOCK
    assert 'file.chunk' in MOCK
    assert 'file.complete' in MOCK
    assert 'close_without_handshake' in MOCK
