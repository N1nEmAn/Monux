from __future__ import annotations

import shutil
import subprocess


def _command_detail(result: subprocess.CompletedProcess[str], backend: str) -> str:
    detail = result.stderr.strip() or result.stdout.strip()
    return detail or f"{backend} exited with code {result.returncode}"


def type_text(text: str) -> tuple[bool, str]:
    if not text:
        return False, "empty text"

    commands = [
        ("xdotool", ["xdotool", "type", "--clearmodifiers", "--", text]),
        ("ydotool", ["ydotool", "type", text]),
    ]
    failures: list[str] = []
    for backend, command in commands:
        if shutil.which(backend) is None:
            continue
        result = subprocess.run(command, capture_output=True, check=False, text=True)
        if result.returncode == 0:
            return True, backend
        failures.append(_command_detail(result, backend))
    if failures:
        return False, failures[-1]
    return False, "xdotool or ydotool not installed"


def press_key(key: str) -> tuple[bool, str]:
    if not key:
        return False, "empty key"
    if shutil.which("xdotool") is None:
        return False, "xdotool not installed"
    result = subprocess.run(["xdotool", "key", key], capture_output=True, check=False, text=True)
    if result.returncode == 0:
        return True, "xdotool"
    return False, _command_detail(result, "xdotool")
