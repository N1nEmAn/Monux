from __future__ import annotations

import socket
import threading
from dataclasses import dataclass
from typing import Optional

from zeroconf import ServiceBrowser, ServiceInfo, ServiceListener, Zeroconf

SERVICE_TYPE = "_minux._tcp.local."


@dataclass(slots=True)
class DiscoveredDevice:
    name: str
    host: str
    port: int

    @property
    def ws_url(self) -> str:
        return f"ws://{self.host}:{self.port}"


class _Listener(ServiceListener):
    def __init__(self) -> None:
        self.event = threading.Event()
        self.device: Optional[DiscoveredDevice] = None

    def add_service(self, zc: Zeroconf, service_type: str, name: str) -> None:
        info = zc.get_service_info(service_type, name)
        if info is None:
            return
        host = _extract_host(info)
        if host is None:
            return
        self.device = DiscoveredDevice(name=name, host=host, port=info.port)
        self.event.set()

    def update_service(self, zc: Zeroconf, service_type: str, name: str) -> None:
        self.add_service(zc, service_type, name)

    def remove_service(self, zc: Zeroconf, service_type: str, name: str) -> None:
        return


def discover_device(timeout: float = 5.0) -> Optional[DiscoveredDevice]:
    zeroconf = Zeroconf()
    listener = _Listener()
    browser = ServiceBrowser(zeroconf, SERVICE_TYPE, listener)
    try:
        listener.event.wait(timeout)
        return listener.device
    finally:
        browser.cancel()
        zeroconf.close()


def _extract_host(info: ServiceInfo) -> Optional[str]:
    if info.parsed_addresses():
        return info.parsed_addresses()[0]
    if info.server:
        try:
            return socket.gethostbyname(info.server.rstrip("."))
        except OSError:
            return None
    return None
