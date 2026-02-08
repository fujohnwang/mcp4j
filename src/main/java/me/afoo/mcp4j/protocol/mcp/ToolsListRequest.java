package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP tools/list request parameters.
 */
public class ToolsListRequest {
    
    @JsonProperty("cursor")
    private String cursor;

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }
}
