# Code Review - mcp4j (2026-02-09 v2)

## 核心问题：传输层实现的是旧版 HTTP+SSE，不是 Streamable HTTP

当前实现的是 2024-11-05 版本的 HTTP+SSE transport（已 deprecated），而不是 2025-03-26 规范定义的 Streamable HTTP transport。

| 方面 | 当前实现 (旧版 HTTP+SSE) | 规范要求 (Streamable HTTP) |
|---|---|---|
| 握手方式 | GET → SSE 流，发送 `event: endpoint` 告知 POST 地址 | 无握手，客户端直接 POST 到 MCP endpoint |
| 会话标识 | URL query param `?sessionId=xxx` | `Mcp-Session-Id` HTTP header |
| 会话创建时机 | GET 时创建 | initialize 响应时通过 response header 分配 |
| POST 响应 | 始终返回 202，响应通过 SSE 流回传 | 可返回 `application/json`（直接响应）或 `text/event-stream`（SSE 流） |
| GET 用途 | 必须先 GET 建立 SSE 连接 | 可选，用于服务端主动推送 |
| DELETE | 通过 query param 识别 session | 通过 `Mcp-Session-Id` header 识别 |

## 协议版本问题

- 硬编码 `PROTOCOL_VERSION = "2024-11-05"`，应为 `"2025-03-26"`
- 缺少 `MCP-Protocol-Version` header 处理

## 安全问题

- 无 Origin header 校验，无条件设置 `Access-Control-Allow-Origin: *`
- 规范要求：Servers MUST validate the Origin header to prevent DNS rebinding attacks

## 具体代码问题

1. **McpServer.Builder** 每次 setter 都重建整个 Config，应直接持有 ConfigBuilder
2. **SessionManager.Session.lastAccessTime** 多线程读写无 volatile/同步保护
3. **ServerCapabilities.ToolsCapability** 缺少 `listChanged` 字段
4. **InitializeResult** 缺少可选的 `instructions` 字段
5. **Implementation 类重复定义** — InitializeRequest 和 InitializeResult 各有一份
6. **tools/list 忽略分页 cursor**
7. **ToolExecutor.execute()** arguments 为 null 时会 NPE
8. **ObjectMapper 实例分散** — SseWriter 和 McpHttpHandler 各自创建
9. **JsonRpcRequest.id 类型为 Object** — 反序列化/序列化可能类型不一致
10. **缺少 JSON-RPC batch 支持**

## 设计建议

1. 传输层与协议处理层应分离，McpHttpHandler 同时处理了 HTTP 和 MCP 协议逻辑
2. `com.sun.net.httpserver` 属于非标准 API，建议文档说明并考虑 SPI 接口
3. 线程池大小硬编码为 10，应可配置
