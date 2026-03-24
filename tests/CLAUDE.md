[根目录](../CLAUDE.md) > **tests**

# tests

## 模块职责
`tests/` 提供 Monux 的回归测试与 mock 对端，当前以 Python 为主，重点覆盖协议握手、文件接收路径、投屏控制器行为，以及 Android 侧部分源码连线约束。

## 入口与启动
- 测试说明：`/home/N1nE/Progress/202603/Monux/TESTING.md`
- 常用命令：
```bash
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_integration.py
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase5_file.py
PYTEST_DISABLE_PLUGIN_AUTOLOAD=1 python3 -m pytest -q tests/test_phase6_screen.py
python3 tests/mock_android.py
```

## 对外接口
- `mock_android.py`
  - 启动本地 WebSocket mock server
  - 返回 `hello_ack`
  - 推送 `ping` 与文件传输样例 `file.offer/file.chunk/file.complete`
- 测试文件：
  - `test_integration.py`
  - `test_phase5_file.py`
  - `test_phase6_screen.py`

## 关键依赖与配置
- 依赖宿主 Python 环境与 `pytest`
- 依赖根目录源码文件可直接读取或动态加载
- 通过 `PYTEST_DISABLE_PLUGIN_AUTOLOAD=1` 减少本地环境差异

## 数据模型
- 无独立业务数据模型
- 主要围绕源码字符串断言、动态 import、临时目录与 stub socket 构造测试夹具

## 测试与质量
### 已覆盖
- 协议常量：`hello`, `ping`, `pong`
- Linux daemon 握手发送与 ping -> pong 逻辑
- Android mock 对端握手与文件流模拟
- 文件接收目录创建、分块写入、完成确认、错误清理
- `scrcpy` 启停与 Android Tile/Manifest 连接检查

### 未充分覆盖
- Android Compose UI 交互
- 通知监听真实系统行为
- 短信真实收发与回复
- 剪贴板跨端全链路
- 远程输入端到端注入

## 常见问题 (FAQ)
### 为什么测试会直接读取源码字符串？
当前项目跨 Android/Kotlin 与 Linux/Python 双端，部分验证采用轻量源码断言来确认协议和集成点存在，避免搭建更重的 Android 测试环境。

### 为什么有 `mock_android.py`？
它为 Linux daemon 提供一个最小化 Android 协议对端，适合本地调试握手、文件接收等流程。

## 相关文件清单
- `README.md`
- `mock_android.py`
- `test_integration.py`
- `test_phase5_file.py`
- `test_phase6_screen.py`

## 变更记录 (Changelog)
- 2026-03-24 17:25:15 — 初始化测试模块文档，汇总测试入口、覆盖范围与主要缺口。
