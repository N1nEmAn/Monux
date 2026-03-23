from __future__ import annotations

import json
import platform
import socket
from dataclasses import dataclass
from typing import Any

PROTOCOL_VERSION = 1
TYPE_HELLO = "hello"
TYPE_HELLO_ACK = "hello_ack"
TYPE_PING = "ping"
TYPE_PONG = "pong"
TYPE_NOTIFY = "notify"
TYPE_CLIPBOARD = "clipboard"


@dataclass(slots=True)
class Envelope:
    type: str
    payload: dict[str, Any]

    def to_json(self) -> str:
        return json.dumps({"type": self.type, "payload": self.payload})

    @classmethod
    def from_json(cls, raw: str) -> "Envelope":
        data = json.loads(raw)
        payload = data.get("payload", {})
        if not isinstance(payload, dict):
            payload = {}
        return cls(type=str(data.get("type", "")), payload=payload)


def local_device_name() -> str:
    return socket.gethostname() or platform.node() or "linux"


def hello(platform_name: str = "linux") -> Envelope:
    return Envelope(
        type=TYPE_HELLO,
        payload={
            "version": PROTOCOL_VERSION,
            "device_name": local_device_name(),
            "platform": platform_name,
        },
    )


def hello_ack(platform_name: str = "linux") -> Envelope:
    return Envelope(
        type=TYPE_HELLO_ACK,
        payload={
            "version": PROTOCOL_VERSION,
            "device_name": local_device_name(),
            "platform": platform_name,
        },
    )


def ping() -> Envelope:
    return Envelope(type=TYPE_PING, payload={"version": PROTOCOL_VERSION})


def pong() -> Envelope:
    return Envelope(type=TYPE_PONG, payload={"version": PROTOCOL_VERSION})
