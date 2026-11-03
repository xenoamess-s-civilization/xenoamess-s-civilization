# cyan_potion MCP

可复用的 Java 17 自动化模块。游戏实现 `AutomationTarget`，在持有游戏状态／图形上下文的线程逐帧调用 `AutomationBridge.drain()`，即可接入命令、状态、截图与事件读取。

独立 `McpServer` 进程通过 stdio 提供 MCP，游戏日志不会写入该进程的协议输出。当前明确支持 MCP `2025-11-25` 握手协议；不宣称实现后续协议版本。传输遵循 [MCP stdio 规范](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)。

## 工具

| 工具 | 用途 |
| --- | --- |
| `engine_status` | 当前游戏状态、运行模式与界面状态 |
| `engine_commands` | 当前游戏支持的命令及参数 schema |
| `engine_command` | 在游戏线程执行命令 |
| `engine_screenshot` | 返回 PNG 图像内容 |
| `engine_events` | 最近 256 次命令及结果，可按 sequence 增量读取 |

本地桥只监听 `127.0.0.1`，每次启动生成随机令牌，写入目标游戏的 `target/automation-session.json`。会话文件不应进入 Git。请求须使用该令牌；不接收浏览器 Origin。队列有数量上限，请求超时后尚未执行的任务会取消。

## 维护要求

新增游戏能力时，同步维护命令 schema、界面入口和自动化测试。新增规则应由界面与自动化调用同一实现；不要增加只有测试路径才能成功的旁路。图形截图必须来自实际窗口；无窗口测试画面应明确标注运行模式。扩展协议时补充握手、错误、通知和真实进程测试。

chess 的接入、启动和测试方法会记录在其模块 README 中。Codex 的项目配置使用 `.codex/config.toml`，参见 [OpenAI 官方 MCP 配置文档](https://developers.openai.com/codex/mcp)。
