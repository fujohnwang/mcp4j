package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP Tool information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolInfo {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("inputSchema")
    private JsonNode inputSchema;

    public ToolInfo(String name, String description, JsonNode inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonNode getInputSchema() {
        return inputSchema;
    }
}
