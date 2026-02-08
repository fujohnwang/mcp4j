package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MCP tools/list response result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolsListResult {
    
    @JsonProperty("tools")
    private List<ToolInfo> tools;
    
    @JsonProperty("nextCursor")
    private String nextCursor;

    public ToolsListResult(List<ToolInfo> tools) {
        this.tools = tools;
    }

    public List<ToolInfo> getTools() {
        return tools;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}
