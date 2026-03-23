from __future__ import annotations

import asyncio
import json
from pathlib import Path

import websockets

HOST = "127.0.0.1"
PORT = 39281


async def run_mock() -> None:
    async with websockets.serve(handle_client, HOST, PORT):
        print(f"mock_android listening on ws://{HOST}:{PORT}")
        await asyncio.Future()


async def handle_client(websocket: websockets.WebSocketServerProtocol) -> None:
    raw = await websocket.recv()
    hello = json.loads(raw)
    print(f"received hello: {hello}")

    await websocket.send(
        json.dumps(
            {
                "type": "hello_ack",
                "payload": {
                    "version": 1,
                    "device_name": "MockAndroid",
                    "platform": "android",
                },
            }
        )
    )

    await websocket.send(json.dumps({"type": "ping", "payload": {"version": 1}}))
    await websocket.send(
        json.dumps(
            {
                "type": "file.offer",
                "payload": {
                    "transfer_id": "phase5-demo",
                    "name": "hello.txt",
                    "size": 11,
                },
            }
        )
    )
    await websocket.send(
        json.dumps(
            {
                "type": "file.chunk",
                "payload": {
                    "transfer_id": "phase5-demo",
                    "index": 0,
                    "total": 1,
                    "data_base64": "aGVsbG8gd29ybGQ=",
                },
            }
        )
    )
    await websocket.send(
        json.dumps(
            {
                "type": "file.complete",
                "payload": {
                    "transfer_id": "phase5-demo",
                    "name": "hello.txt",
                },
            }
        )
    )

    async for message in websocket:
        parsed = json.loads(message)
        print(f"daemon -> mock_android: {parsed}")


if __name__ == "__main__":
    try:
        asyncio.run(run_mock())
    except KeyboardInterrupt:
        print("mock_android stopped")
