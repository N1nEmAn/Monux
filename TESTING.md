# TESTING

This document describes the local verification flow for Monux Phases 1-6.

## Environment

### Linux daemon
```bash
cd linux-daemon
pip install -r requirements.txt
python3 main.py
```

### Test runner
Use plugin autoload disabled for stable local pytest runs:
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q <test-file>
```

## Phase 1 — Communication Layer

### Coverage
- WebSocket hello / hello_ack handshake
- ping / pong protocol path
- mock Android protocol peer structure

### Files
- `tests/test_integration.py`
- `tests/mock_android.py`

### Run
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_integration.py
python3 tests/mock_android.py
```

### What it validates
- daemon sends `hello`
- daemon responds to `ping` with `pong`
- mock Android side contains hello/file-flow protocol fixtures

## Phase 2 — Notification Mirroring

### Coverage
- Source-level integration coverage via protocol and service wiring
- Desktop notification execution path is exercised by daemon handlers during manual runs

### Relevant files
- `android-app/app/src/main/kotlin/com/monux/notify/MonuxNotificationListener.kt`
- `linux-daemon/main.py`

### Manual run
- Install Android app
- Grant notification access
- Trigger a phone notification
- Confirm Linux `notify-send` popup and daemon log

## Phase 3 — Clipboard Sync

### Coverage
- Bidirectional clipboard path with content-hash loop prevention
- Clipboard handler behavior is exercised through daemon/message path and manual system integration

### Relevant files
- `linux-daemon/handlers/clipboard.py`
- `linux-daemon/watchers/clipboard_watch.py`
- `android-app/app/src/main/kotlin/com/monux/clipboard/ClipboardMonitor.kt`
- `android-app/app/src/main/kotlin/com/monux/clipboard/ClipboardSyncManager.kt`

### Manual run
- Start daemon and Android app
- Copy text on Android and verify Linux clipboard changes
- Copy text on Linux and verify Android clipboard changes

## Phase 4 — SMS Mirroring / Reply

### Coverage
- SMS mirror and reply path validated by code path and daemon handler behavior

### Relevant files
- `android-app/app/src/main/kotlin/com/monux/sms/SmsReceiver.kt`
- `android-app/app/src/main/kotlin/com/monux/sms/SmsReplyExecutor.kt`
- `linux-daemon/handlers/sms.py`

### Manual run
- Send an SMS to the Android device
- Confirm Linux receives desktop popup
- Reply from Linux rofi prompt and verify Android send result

## Phase 5 — File Transfer + Share Sheet

### Coverage
- file offer / chunk / complete / error receive path
- file save behavior on Linux
- mock Android file-transfer protocol sequence

### Files
- `tests/test_phase5_file.py`
- `tests/mock_android.py`

### Run
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase5_file.py
```

### Expected
- `4 passed`

### What it validates
- target directory creation
- chunk append behavior
- offer/chunk/complete lifecycle
- transfer cleanup on error
- receipt acknowledgement message generation

## Phase 6 — Screen Mirror / scrcpy

### Coverage
- scrcpy wrapper start/stop behavior
- not-installed path
- already-running path
- Quick Settings Tile wiring
- Android manifest QS tile registration

### Files
- `tests/test_phase6_screen.py`

### Run
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase6_screen.py
```

### Expected
- `5 passed`

### What it validates
- scrcpy wrapper error path when binary missing
- scrcpy start command generation
- stop lifecycle
- Quick Settings Tile source wiring
- manifest tile registration

## Final Summary

### Verified test commands
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase5_file.py
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase6_screen.py
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_integration.py
```

### Confirmed results
- `tests/test_phase5_file.py` → 4 passed, 2 warnings
- `tests/test_phase6_screen.py` → 5 passed
- `tests/test_integration.py` → 3 passed
