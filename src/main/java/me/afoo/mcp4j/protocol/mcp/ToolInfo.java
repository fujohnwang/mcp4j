package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * MCP Tool information returned in tools/list response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolInfo {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inputSchema")
    private JsonNode inputSchema;

    @JsonProperty("annotations")
    private ToolAnnotations annotations;

    public ToolInfo(String name, String description, JsonNode inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public ToolInfo(String name, String description, JsonNode inputSchema, ToolAnnotations annotations) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.annotations = annotations;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public JsonNode getInputSchema() { return inputSchema; }
    public ToolAnnotations getAnnotations() { return annotations; }
}
