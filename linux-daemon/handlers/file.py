from __future__ import annotations

import base64
from dataclasses import dataclass
from pathlib import Path
import subprocess


@dataclass(slots=True)
class IncomingFileTransfer:
    transfer_id: str
    name: str
    target_dir: Path

    @property
    def path(self) -> Path:
        return self.target_dir / self.name


def ensure_target_dir(path: str) -> Path:
    target = Path(path).expanduser()
    target.mkdir(parents=True, exist_ok=True)
    return target


def write_chunk(transfer: IncomingFileTransfer, data_base64: str) -> None:
    raw = base64.b64decode(data_base64)
    with transfer.path.open("ab") as fp:
        fp.write(raw)


def notify_file_received(path: Path) -> None:
    subprocess.run(["notify-send", "Monux 文件已接收", str(path)], check=False)
