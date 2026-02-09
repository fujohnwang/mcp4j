# MCP 项目代码审查报告 (2025 规范版)

## 1. 总体评价
当前项目已演进至 **2025-03-26** 协议版本，并采用了 2025 规范中极具前瞻性的 **"Streamable HTTP"** 传输模式。相比传统的 SSE（服务器发送事件），该实现更加符合现代云原生和无状态架构的趋势，通过显式的会话管理实现了复杂的双向通信逻辑。

## 2. 核心架构分析

### 2.1 传输层：Streamable HTTP 的实现
*   **亮点**: 放弃了复杂的 SSE 长连接，转而使用 `POST` 接收请求并直接返回 `application/json` 响应。这是 2025 规范中推荐的轻量级方案。
*   **会话管理**: 使用自定义 Header `Mcp-Session-Id` 来追踪上下文，这在处理多轮对话和工具调用状态时非常高效。
*   **动作一致性**: 实现了 `DELETE` 方法用于显式终止会话，符合规范中对资源清理的要求。

### 2.2 协议兼容性
*   **版本号**: `PROTOCOL_VERSION` 已更新为 `2025-03-26`，处于规范的最前沿。
*   **JSON-RPC 语义**: 正确处理了 `notifications/initialized` 等通知类消息（不返回 Body，返回 202 Accepted），这体现了对协议细节的深度理解。

## 3. 2025 规范项下的差距分析 (Gap Analysis)

### 3.1 提示词启发 (Elicitation) —— **缺失**
*   **规范要求**: 2025 规范引入了 `elicitation` 机制，允许服务器在工具执行过程中“暂停”，请求用户补充输入（如：权限确认、额外参数）。
*   **现状**: 当前 `ToolExecutor` 是同步阻塞执行的，不支持这种“执行中交互”的流程。

### 3.2 结构化内容 (Structured Content) —— **部分支持**
*   **规范要求**: 新规范支持 `structuredContent`，允许工具返回复杂的 JSON 对象而不仅仅是字符串或图片。
*   **现状**: `ToolsCallResult` 目前主要通过 `text` 和 `image` 承载数据，对原始 JSON 结构的透传支持尚不完善。

### 3.3 安全性 (Security & OAuth 2.1) —— **缺失**
*   **规范要求**: 2025 规范强调服务器应作为 OAuth 2.1 资源服务器运行，强制要求 Token 绑定和 Scope 控制。
*   **现状**: 项目目前仅依赖简单的 `Mcp-Session-Id`，缺乏企业级的身份验证和授权机制。

### 3.4 元数据与 UI 增强 —— **缺失**
*   **规范要求**: 服务器可以为工具和提示词提供 `icons`（图标）元数据，以优化 AI 客户端的展示效果。
*   **现状**: `ToolInfo` 仅包含基础的 `name`、`description` 和 `schema`。

## 4. 内部代码质量见解

1.  **代码冗余**: `src/main/java/me/afoo/mcp4j/transport/SseWriter.java` 在目前的 "Streamable HTTP" 架构下已完全失效。由于 `McpHttpHandler` 对 `GET` 请求返回 405，该类及其相关逻辑应考虑移除或标记为弃用。
2.  **错误处理**: `McpHttpHandler` 中的异常捕获较为笼统。建议细化错误分类，根据 MCP 规范返回标准的 JSON-RPC Error Codes。
3.  **并发性能**: 目前使用 JDK 原生的 `HttpServer`，未来可以考虑迁移至高性能异步框架（如 Vert.x 或 Netty）。

## 5. 总结与建议

**结论**: `mcp4j` 是目前 Java 圈内对 MCP 2025 规范响应最快的项目之一，其核心的 Streamable HTTP 实现非常稳健。

**建议方向**:
*   **移除 SSE 残留**: 清理不再使用的 SSE 代码。
*   **引入 Elicitation**: 为 `ToolHandler` 增加异步回调或中断机制。
*   **增强 Content 类型**: 增加对 `structuredContent` 的原生支持。

---
**报告人**: Gemini CLI Agent
**日期**: 2026-02-09
