from __future__ import annotations

import importlib.util
import sys
from pathlib import Path
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[1]
LINUX_DAEMON = ROOT / "linux-daemon"
sys.path.insert(0, str(LINUX_DAEMON))

screen_spec = importlib.util.spec_from_file_location("monux_screen", LINUX_DAEMON / "handlers" / "screen.py")
screen = importlib.util.module_from_spec(screen_spec)
assert screen_spec and screen_spec.loader
sys.modules["monux_screen"] = screen
screen_spec.loader.exec_module(screen)


def test_scrcpy_start_returns_not_installed_when_missing(monkeypatch) -> None:
    controller = screen.ScrcpyController()
    monkeypatch.setattr(screen.shutil, "which", lambda name: None)
    success, detail = controller.start(screen.ScreenConfig())
    assert success is False
    assert detail == "scrcpy not installed"


def test_scrcpy_start_and_stop(monkeypatch) -> None:
    controller = screen.ScrcpyController()
    calls: list[list[str]] = []

    class DummyProcess:
        def __init__(self) -> None:
            self._running = True

        def poll(self):
            return None if self._running else 0

        def terminate(self):
            self._running = False

        def wait(self, timeout: int):
            self._running = False
            return 0

    monkeypatch.setattr(screen.shutil, "which", lambda name: "/usr/bin/scrcpy")
    monkeypatch.setattr(
        screen.subprocess,
        "Popen",
        lambda args, text=True: calls.append(args) or DummyProcess(),
    )

    success, detail = controller.start(screen.ScreenConfig(max_size=1200, bitrate="4M"))
    assert success is True
    assert detail == "scrcpy started"
    assert calls == [["scrcpy", "--max-size", "1200", "--video-bit-rate", "4M"]]
    assert controller.running() is True

    success, detail = controller.stop()
    assert success is True
    assert detail == "scrcpy stopped"
    assert controller.running() is False


def test_scrcpy_start_returns_already_running(monkeypatch) -> None:
    controller = screen.ScrcpyController()

    class DummyProcess:
        def poll(self):
            return None

    monkeypatch.setattr(screen.shutil, "which", lambda name: "/usr/bin/scrcpy")
    controller._process = DummyProcess()
    success, detail = controller.start(screen.ScreenConfig())
    assert success is True
    assert detail == "scrcpy already running"


def test_screen_tile_source_contains_qs_tile_toggle() -> None:
    tile_path = ROOT / "android-app" / "app" / "src" / "main" / "kotlin" / "com" / "monux" / "screen" / "ScreenTileService.kt"
    content = tile_path.read_text(encoding="utf-8")
    assert "TileService" in content
    assert "MainService.toggleScreenMirror()" in content
    assert "Tile.STATE_ACTIVE" in content


def test_manifest_registers_screen_tile_service() -> None:
    manifest = (ROOT / "android-app" / "app" / "src" / "main" / "AndroidManifest.xml").read_text(encoding="utf-8")
    assert ".screen.ScreenTileService" in manifest
    assert "android.service.quicksettings.action.QS_TILE" in manifest
    assert "android.permission.BIND_QUICK_SETTINGS_TILE" in manifest
