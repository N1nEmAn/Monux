[根目录](../CLAUDE.md) > **android-app**

# android-app

## 模块职责
Android 应用模块负责在手机侧暴露 Monux 的系统能力：以前台服务常驻、通过 WebSocket 服务端对外提供连接、通过 mDNS 广播服务地址，并把通知、短信、分享文件、投屏控制、远程输入等能力桥接到 Linux 端。

## 入口与启动
- 应用入口：`app/src/main/kotlin/com/monux/MainActivity.kt`
  - 启动时调用 `ContextCompat.startForegroundService(... MainService::class.java)`。
  - Compose UI 通过 `MainService.stateFlow()` 读取状态。
- 核心服务：`app/src/main/kotlin/com/monux/MainService.kt`
  - 创建 `WebSocketServer`
  - 注册 mDNS `_monux._tcp.`
  - 启动剪贴板监听与心跳广播
  - 接收分享文件、转发通知、短信、投屏和远程输入事件
- 服务端网络入口：`app/src/main/kotlin/com/monux/network/WebSocketServer.kt`
  - 基于 `NanoWSD`
  - 客户端连接后发送 `hello`
  - 处理 `hello` / `ping` 并上报其他消息给 `MainService`

## 对外接口
### 协议
- 协议定义：`app/src/main/kotlin/com/monux/protocol/Protocol.kt`
- 已识别消息类型：
  - 基础连接：`hello`, `hello_ack`, `ping`, `pong`
  - 通知：`notify`
  - 剪贴板：`clipboard`
  - 短信：`sms`, `sms.send`, `sms.sent`
  - 文件：`file.offer`, `file.chunk`, `file.complete`, `file.received`, `file.error`
  - 投屏：`screen.start`, `screen.stop`, `screen.started`
  - 输入：`input.text`, `input.key`, `input.voice`

### Android 系统入口
- 通知监听：`notify/MonuxNotificationListener.kt`
- 短信广播接收：`sms/SmsReceiver.kt`
- 分享菜单接入：`share/ShareReceiverActivity.kt`
- Quick Settings Tile：`screen/ScreenTileService.kt`, `input/InputTileService.kt`
- 远程输入界面：`input/RemoteInputActivity.kt`
- Manifest 暴露点：`app/src/main/AndroidManifest.xml`

## 关键依赖与配置
- 构建配置：`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`
- 主要依赖：
  - Jetpack Compose / Material 3
  - `org.nanohttpd:nanohttpd-websocket`
  - `kotlinx-coroutines-android`
- 主要权限：
  - `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`
  - `POST_NOTIFICATIONS`
  - `READ_SMS`, `SEND_SMS`, `READ_CONTACTS`
  - `INTERNET`, `ACCESS_NETWORK_STATE`, `CHANGE_WIFI_MULTICAST_STATE`
  - `RECORD_AUDIO`
- 服务注册：
  - `MainService`
  - `MonuxNotificationListener`
  - `ScreenTileService`
  - `InputTileService`
  - `SmsReceiver`

## 数据模型
- UI 状态集中在 `ui/state/ConnectionState.kt`
  - `FeatureFlags`：`notifications`, `clipboard`, `sms`, `file`, `screen`, `remoteInput`
  - `FileTransferState`
  - `ScreenState`
  - `UiPreferences`
  - `ConnectionState`
- 无数据库、ORM、迁移或独立 schema 文件。
- 文件传输采用内存读取 + Base64 分块发送：`file/FileTransferManager.kt`
- 剪贴板同步通过 SHA-256 内容哈希防止循环回写：`clipboard/ClipboardSyncManager.kt`

## 测试与质量
- 仓库内未发现 Android 侧独立单元测试或 instrumentation 测试目录。
- 现有 Python 测试会间接校验以下 Android 文件存在关键实现：
  - `screen/ScreenTileService.kt`
  - `AndroidManifest.xml`
  - `tests/mock_android.py` 中的协议夹具
- 质量工具缺口：未发现 `detekt`/`ktlint` 配置。
- 主要风险点：
  - `MainService` 责任较重，承担网络、状态、系统接入与部分业务编排。
  - 文件发送为整文件读入内存，超大文件场景可能产生内存压力。
  - 远程输入依赖 Linux 端已有连接与工具链可用，Android 侧仅做前置校验。

## 常见问题 (FAQ)
### 为什么 Android 端是 WebSocket 服务端而不是客户端？
当前设计让 Android 端在局域网内通过 mDNS 广播自身地址，Linux daemon 自动发现并主动连入，减少手机端手工输入地址。

### 功能开关在哪里生效？
主要在 `MainService.updateFeatureFlags` 与对应转发函数中生效，例如 `forwardNotification`、`forwardSms`、`toggleScreenMirror`、远程输入发送函数。

### 远程输入如何触发？
可从 `RemoteInputActivity` 手动输入/语音输入，也可通过 `InputTileService` 从 Quick Settings 入口打开。

## 相关文件清单
- `app/src/main/kotlin/com/monux/MainActivity.kt`
- `app/src/main/kotlin/com/monux/MainService.kt`
- `app/src/main/kotlin/com/monux/network/WebSocketServer.kt`
- `app/src/main/kotlin/com/monux/protocol/Protocol.kt`
- `app/src/main/kotlin/com/monux/clipboard/ClipboardSyncManager.kt`
- `app/src/main/kotlin/com/monux/file/FileTransferManager.kt`
- `app/src/main/kotlin/com/monux/notify/MonuxNotificationListener.kt`
- `app/src/main/kotlin/com/monux/notify/NotificationPayloadMapper.kt`
- `app/src/main/kotlin/com/monux/sms/SmsReceiver.kt`
- `app/src/main/kotlin/com/monux/sms/SmsReplyExecutor.kt`
- `app/src/main/kotlin/com/monux/screen/ScreenTileService.kt`
- `app/src/main/kotlin/com/monux/screen/ScreenSessionManager.kt`
- `app/src/main/kotlin/com/monux/input/RemoteInputActivity.kt`
- `app/src/main/kotlin/com/monux/input/InputTileService.kt`
- `app/src/main/kotlin/com/monux/ui/screens/HomeScreen.kt`
- `app/src/main/kotlin/com/monux/ui/screens/FilesScreen.kt`
- `app/src/main/kotlin/com/monux/ui/screens/SettingsTab.kt`
- `app/src/main/AndroidManifest.xml`

## 变更记录 (Changelog)
- 2026-03-24 17:25:15 — 初始化模块文档，归纳 Android 服务入口、协议接口、权限与测试缺口。
