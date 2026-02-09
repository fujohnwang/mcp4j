package me.afoo.mcp4j.tool;

import com.fasterxml.jackson.databind.JsonNode;
import me.afoo.mcp4j.protocol.mcp.ToolAnnotations;

/**
 * Represents an MCP tool with its metadata and execution handler.
 */
public class Tool {
    private final String name;
    private final String description;
    private final JsonNode inputSchema;
    private final ToolHandler handler;
    private final ToolAnnotations annotations;

    private Tool(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.inputSchema = builder.inputSchema;
        this.handler = builder.handler;
        this.annotations = builder.annotations;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public JsonNode getInputSchema() { return inputSchema; }
    public ToolHandler getHandler() { return handler; }
    public ToolAnnotations getAnnotations() { return annotations; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private JsonNode inputSchema;
        private ToolHandler handler;
        private ToolAnnotations annotations;

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder inputSchema(JsonNode inputSchema) { this.inputSchema = inputSchema; return this; }
        public Builder handler(ToolHandler handler) { this.handler = handler; return this; }
        public Builder annotations(ToolAnnotations annotations) { this.annotations = annotations; return this; }

        public Tool build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Tool name is required");
            }
            if (handler == null) {
                throw new IllegalArgumentException("Tool handler is required");
            }
            return new Tool(this);
        }
    }
}
