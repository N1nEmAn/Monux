from __future__ import annotations

import hashlib
import shutil
import subprocess
from dataclasses import dataclass


@dataclass(slots=True)
class ClipboardPayload:
    text: str
    content_hash: str


def compute_content_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def read_clipboard() -> ClipboardPayload | None:
    commands = [
        ["wl-paste", "--no-newline"],
        ["xclip", "-selection", "clipboard", "-o"],
        ["xsel", "--clipboard", "--output"],
    ]
    for command in commands:
        if shutil.which(command[0]) is None:
            continue
        result = subprocess.run(command, capture_output=True, check=False, text=True)
        if result.returncode == 0:
            text = result.stdout
            return ClipboardPayload(text=text, content_hash=compute_content_hash(text))
    return None


def write_clipboard(text: str) -> str | None:
    commands = [
        ["wl-copy"],
        ["xclip", "-selection", "clipboard"],
        ["xsel", "--clipboard", "--input"],
    ]
    for command in commands:
        if shutil.which(command[0]) is None:
            continue
        result = subprocess.run(command, input=text.encode("utf-8"), check=False)
        if result.returncode == 0:
            return compute_content_hash(text)
    return None
