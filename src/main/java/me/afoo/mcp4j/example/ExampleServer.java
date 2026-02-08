package me.afoo.mcp4j.example;

import me.afoo.mcp4j.schema.JsonSchema;
import me.afoo.mcp4j.server.McpServer;
import me.afoo.mcp4j.tool.Tool;

import java.time.Duration;
import java.time.Instant;

/**
 * Example MCP server demonstrating multiple tools.
 */
public class ExampleServer {

    public static void main(String[] args) {
        try {
            McpServer server = McpServer.builder()
                .host("127.0.0.1")
                .port(8080)
                .endpoint("/mcp")
                .sessionTimeout(Duration.ofMinutes(30))
                .serverName("example-mcp-server")
                .serverVersion("1.0.0")
                // Tool 1: Echo - Simple text echo
                .tool(Tool.builder()
                    .name("echo")
                    .description("Echo back the input message")
                    .inputSchema(JsonSchema.object()
                        .property("message", JsonSchema.string()
                            .description("Message to echo back")
                            .required())
                        .build())
                    .handler(params -> {
                        return params.get("message");
                    })
                    .build())
                // Tool 2: Add - Calculator addition
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
                // Tool 3: Get Time - Current server time
                .tool(Tool.builder()
                    .name("get_time")
                    .description("Get current server time in specified format")
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
                        } else {
                            return Instant.now().toString();
                        }
                    })
                    .build())
                // Tool 4: Reverse String
                .tool(Tool.builder()
                    .name("reverse")
                    .description("Reverse a string")
                    .inputSchema(JsonSchema.object()
                        .property("text", JsonSchema.string()
                            .description("Text to reverse")
                            .required())
                        .build())
                    .handler(params -> {
                        String text = (String) params.get("text");
                        return new StringBuilder(text).reverse().toString();
                    })
                    .build())
                // Tool 5: String Length
                .tool(Tool.builder()
                    .name("length")
                    .description("Get the length of a string")
                    .inputSchema(JsonSchema.object()
                        .property("text", JsonSchema.string()
                            .description("Text to measure")
                            .required())
                        .build())
                    .handler(params -> {
                        String text = (String) params.get("text");
                        return text.length();
                    })
                    .build())
                .build();

            // Start server
            server.start();
            System.out.println("MCP Server started at: " + server.getAddress());
            System.out.println("Press Ctrl+C to stop...");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                server.stop();
                System.out.println("Server stopped.");
            }));

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
