# Intro to MCP4j

MCP Server impl. support in Java Ecosystem sucks, they (spring ai and other java frameworks) bring too much nasty things of their own.

In fact, to bring up a MCP server in Java should be easssssssy

That's why I present you this **LIBRARY**: 

- it's light weight without unnecessary dependencies 
- it's easy to use
- it's AI agent ready

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>me.afoo</groupId>
    <artifactId>mcp4j</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Example: Create a Simple MCP Server

```java
import me.afoo.mcp4j.schema.JsonSchema;
import me.afoo.mcp4j.server.McpServer;
import me.afoo.mcp4j.tool.Tool;

import java.time.Duration;

public class ExampleServer {
    public static void main(String[] args) throws Exception {
        McpServer server = McpServer.builder()
            .host("127.0.0.1")
            .port(8080)
            .endpoint("/mcp")
            .sessionTimeout(Duration.ofMinutes(30))
            // Tool 1: Echo
            .tool(Tool.builder()
                .name("echo")
                .description("Echo back the input message")
                .inputSchema(JsonSchema.object()
                    .property("message", JsonSchema.string()
                        .description("Message to echo back")
                        .required())
                    .build())
                .handler(params -> params.get("message"))
                .build())
            // Tool 2: Add numbers
            .tool(Tool.builder()
                .name("add")
                .description("Add two numbers together")
                .inputSchema(JsonSchema.object()
                    .property("a", JsonSchema.number()
                        .description("First number")
                        .required())
                    .property("b", JsonSchema.number()
                        .description("Second number")
                        .required())
                    .build())
                .handler(params -> {
                    double a = ((Number) params.get("a")).doubleValue();
                    double b = ((Number) params.get("b")).doubleValue();
                    return a + b;
                })
                .build())
            // Tool 3: Get current time
            .tool(Tool.builder()
                .name("get_time")
                .description("Get current server time")
                .inputSchema(JsonSchema.object()
                    .property("format", JsonSchema.string()
                        .description("Time format: iso8601 or unix")
                        .enumValues("iso8601", "unix")
                        .defaultValue("iso8601"))
                    .build())
                .handler(params -> {
                    String format = (String) params.getOrDefault("format", "iso8601");
                    if ("unix".equals(format)) {
                        return System.currentTimeMillis();
                    }
                    return java.time.Instant.now().toString();
                })
                .build())
            .build();

        server.start();
        System.out.println("MCP Server started at: " + server.getAddress());
        
        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        Thread.currentThread().join();
    }
}
```

### Run the Example

```bash
mvn exec:java -Dexec.mainClass="me.afoo.mcp4j.example.ExampleServer"
```

### Test with curl

```bash
# Initialize
curl -X POST http://127.0.0.1:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "initialize",
    "params": {
      "protocolVersion": "2025-03-26",
      "capabilities": {},
      "clientInfo": {"name": "test-client", "version": "1.0.0"}
    }
  }'

# List tools
curl -X POST http://127.0.0.1:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: <session-id-from-initialize>" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/list",
    "params": {}
  }'

# Call a tool
curl -X POST http://127.0.0.1:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: <session-id-from-initialize>" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "add",
      "arguments": {"a": 10, "b": 32}
    }
  }'
```
