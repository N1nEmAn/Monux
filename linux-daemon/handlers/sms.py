from __future__ import annotations

import shlex
import shutil
import subprocess


def mirror_sms(sender: str, body: str) -> None:
    subprocess.run(["notify-send", f"SMS from {sender}", body], check=False)


def prompt_sms_reply(sender: str, body: str) -> str | None:
    if shutil.which("rofi") is None:
        return None
    command = [
        "rofi",
        "-dmenu",
        "-p",
        f"Reply to {sender}",
        "-mesg",
        body,
    ]
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        return None
    reply = result.stdout.strip()
    return reply or None
