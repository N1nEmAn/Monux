from __future__ import annotations

import shutil
import subprocess
from dataclasses import dataclass


@dataclass(slots=True)
class ScreenConfig:
    max_size: int = 1600
    bitrate: str = "8M"


class ScrcpyController:
    def __init__(self) -> None:
        self._process: subprocess.Popen[str] | None = None

    def start(self, config: ScreenConfig) -> tuple[bool, str]:
        if shutil.which("scrcpy") is None:
            return False, "scrcpy not installed"
        if self._process and self._process.poll() is None:
            return True, "scrcpy already running"
        self._process = subprocess.Popen(
            ["scrcpy", "--max-size", str(config.max_size), "--video-bit-rate", config.bitrate],
            text=True,
        )
        return True, "scrcpy started"

    def stop(self) -> tuple[bool, str]:
        if self._process is None or self._process.poll() is not None:
            return True, "scrcpy already stopped"
        self._process.terminate()
        self._process.wait(timeout=5)
        self._process = None
        return True, "scrcpy stopped"

    def running(self) -> bool:
        return self._process is not None and self._process.poll() is None
