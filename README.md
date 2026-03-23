# Monux

Monux bridges your Linux desktop and Android phone — notifications, clipboard, SMS, file transfer, screen mirror, and remote input. No KDE required.

## Phase 1-6 Overview

Monux Phase 1 establishes the communication layer with local WebSocket transport, mDNS discovery, heartbeat, and reconnect behavior between Android and Linux. Phase 2 adds notification mirroring so Android notifications surface on Linux with desktop popups. Phase 3 adds bidirectional clipboard sync with content-hash loop prevention. Phase 4 adds SMS mirroring and Linux-side reply flow. Phase 5 adds file transfer through Android Share Sheet and Linux-side save/notify behavior. Phase 6 adds scrcpy-based screen mirroring with Quick Settings Tile control and in-app screen settings.

## Quick Start

### Linux daemon
```bash
cd linux-daemon
pip install -r requirements.txt
python3 main.py
```

### Android app
1. Open `android-app/` in Android Studio
2. Build and install the APK
3. Grant the required permissions for notifications, SMS, and network access

## Local Test

### Phase 5
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase5_file.py
```

### Phase 6
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase6_screen.py
```

### Integration handshake smoke test
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_integration.py
```

### Mock Android peer
```bash
python3 tests/mock_android.py
```
