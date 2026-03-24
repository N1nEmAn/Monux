[根目录](../CLAUDE.md) > **linux-daemon**

# linux-daemon

## 模块职责
`linux-daemon` 是 Monux 的 Linux 侧常驻进程，负责通过 mDNS 发现 Android 设备、建立 WebSocket 连接、发送握手与心跳，并把 Android 侧发来的消息落地到本机系统能力，包括桌面通知、剪贴板、文件接收、短信回复、投屏控制和远程输入。

## 入口与启动
- 启动入口：`main.py`
- 启动方式：
```bash
cd linux-daemon
pip install -r requirements.txt
python3 main.py
```
- 主流程：
  1. `MonuxDaemon.run()` 启动 Live dashboard
  2. `_resolve_device_url()` 优先使用环境变量，否则通过 `mdns_discover.discover_device()` 自动发现 Android
  3. 建立 WebSocket 连接后发送 `hello`
  4. 启动心跳与 `ClipboardWatcher`
  5. 通过 `MessageDispatcher` 按消息类型分发处理

## 对外接口
### WebSocket / 协议
- 协议文件：`protocol.py`
- 关键消息：
  - 基础：`hello`, `hello_ack`, `ping`, `pong`
  - 剪贴板：`clipboard`
  - 通知：`notify`
  - 短信：`sms`, `sms.sent`
  - 文件：`file.offer`, `file.chunk`, `file.complete`, `file.error`, 回执 `file.received`
  - 投屏：`screen.start`, `screen.stop`, 回执 `screen.started`
  - 远程输入：`input.text`, `input.key`, `input.voice`

### 环境变量
- `MINUX_RECONNECT_DELAY`
- `MINUX_HEARTBEAT_INTERVAL`
- `MINUX_DISCOVERY_TIMEOUT`
- `MINUX_CLIPBOARD_POLL_INTERVAL`
- `MINUX_DEVICE_URL`
- `MINUX_SAVE_DIR`

## 关键依赖与配置
- 依赖定义：`requirements.txt`
  - `websockets`
  - `zeroconf`
  - `rich`
- 外部系统工具依赖：
  - 通知：`notify-send`
  - 剪贴板：`wl-paste`, `wl-copy`, `xclip`, `xsel`
  - 投屏：`scrcpy`
  - 输入：`xdotool`, `ydotool`
  - 短信回复 UI：`rofi`

## 数据模型
- `protocol.Envelope`：统一封装 `{type, payload}` JSON 消息
- `main.MonuxConfig`：运行配置
- `main.DashboardState`：终端 dashboard 状态
- `handlers.file.IncomingFileTransfer`：文件接收上下文
- `handlers.clipboard.ClipboardPayload`：剪贴板文本与内容哈希
- `handlers.screen.ScreenConfig`：投屏参数
- 无数据库、ORM、schema、迁移目录。

## 测试与质量
- 测试覆盖：
  - `tests/test_integration.py`：握手与 ping/pong 基础协议路径
  - `tests/test_phase5_file.py`：文件接收生命周期
  - `tests/test_phase6_screen.py`：`scrcpy` 控制器、Tile/Manifest 关联断言
- 代码特点：
  - `MessageDispatcher` 在 `main.py` 内集中注册 handler
  - handler 主要是系统命令包装层，副作用明显，自动化测试多以局部行为和源码断言为主
- 质量缺口：
  - 通知镜像、短信回复、远程输入的真实系统集成路径仍缺自动化端到端测试
  - 输入与通知等功能高度依赖宿主桌面环境和外部命令可用性

## 常见问题 (FAQ)
### Linux 端如何发现 Android 端？
默认使用 `mdns_discover.py` 中的 Zeroconf 浏览 `_monux._tcp.local.` 服务；若设置 `MINUX_DEVICE_URL`，则跳过发现直接连接。

### 剪贴板如何避免循环同步？
`ClipboardWatcher` 记录最后一次哈希；收到 Android 下发内容后，`write_clipboard()` 返回应用哈希，再由 watcher `remember_hash()` 标记，避免重复回推。

### 远程输入如何工作？
`handle_input_text()` 调用 `handlers/input.py::type_text`，优先 `xdotool`，其次 `ydotool`；`handle_input_key()` 依赖 `xdotool key`。

### 文件保存到哪里？
默认目录来自 `MINUX_SAVE_DIR`，默认值为 `~/Downloads/Monux`，由 `ensure_target_dir()` 自动创建。

## 相关文件清单
- `main.py`
- `protocol.py`
- `mdns_discover.py`
- `watchers/clipboard_watch.py`
- `handlers/clipboard.py`
- `handlers/file.py`
- `handlers/input.py`
- `handlers/screen.py`
- `handlers/sms.py`
- `requirements.txt`

## 变更记录 (Changelog)
- 2026-03-24 17:25:15 — 初始化模块文档，整理运行入口、消息分发、系统工具依赖与自动化测试覆盖。
