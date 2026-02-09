# Refactoring Checklist - 2026-02-09

## 本次执行

- [x] 3.2 Structured Content — ToolsCallResult 增加 structuredContent (JsonNode) 字段，补齐 audio/resource content 类型
- [x] 3.4 元数据增强 — 新增 ToolAnnotations 类 (readOnlyHint/destructiveHint/idempotentHint/openWorldHint)，ToolInfo 和 Tool 均支持 annotations
- [x] 4.2 错误处理细化 — ToolExecutor 区分 protocol errors (ToolNotFoundException/InvalidToolArgumentsException) 和 tool execution errors (isError=true)，McpHttpHandler 分别映射为 JSON-RPC error 和 tool result

## 后续迭代

- [ ] 3.1 Elicitation — ToolHandler 支持执行中交互（需传输层配合 SSE 流）
- [ ] 3.3 OAuth 2.1 — 提供认证拦截器 SPI，让用户自行插入认证逻辑
