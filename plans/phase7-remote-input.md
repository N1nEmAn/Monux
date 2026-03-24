# Phase 7 - Remote Keyboard / Voice Input

## Goal
Allow user to type or speak on Android, text gets injected into the focused Linux window via xdotool.

## Protocol
Reuse existing WebSocket JSON protocol. New message type:
```json
{"type": "input.text", "text": "hello world"}
{"type": "input.key", "key": "Return"}
{"type": "input.voice", "text": "transcribed text from speech"}
```

## Steps

- [ ] Step 1: Linux daemon - add input handler
  - Create linux-daemon/handlers/input.py
  - Handle input.text: subprocess.run(['xdotool', 'type', '--clearmodifiers', '--', text])
  - Handle input.key: subprocess.run(['xdotool', 'key', key])
  - Register handler in linux-daemon/main.py
  - Add xdotool to install dependencies in README

- [ ] Step 2: Android - RemoteInputActivity
  - Create app/src/main/kotlin/com/monux/input/RemoteInputActivity.kt
  - Full-screen keyboard input field + Send button
  - Voice input button using SpeechRecognizer API
  - On send: WebSocket message {type: input.text, text: ...}
  - On voice result: same

- [ ] Step 3: Android - Quick Settings Tile
  - Create InputTileService extending TileService
  - On tile click: start RemoteInputActivity
  - Register in AndroidManifest.xml

- [ ] Step 4: Android - Feature toggle integration
  - Add remoteInput: Boolean to FeatureFlags in ConnectionState.kt
  - Add toggle in FeatureToggleCard grid (HomeScreen)
  - Wire toggle to MainService

- [ ] Step 5: Integration test
  - Verify xdotool is available on Linux
  - Test type flow end-to-end
  - Build assembleDebug, confirm no crash

## Quality Bar
- xdotool type works on X11 and Wayland (via xdotool + ydotool fallback)
- Voice input triggers SpeechRecognizer correctly
- No crash on unsupported devices
- Feature toggle disables the tile when off
