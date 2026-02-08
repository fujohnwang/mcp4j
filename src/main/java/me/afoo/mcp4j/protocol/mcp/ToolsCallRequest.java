package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * MCP tools/call request parameters.
 */
public class ToolsCallRequest {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("arguments")
    private Map<String, Object> arguments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
