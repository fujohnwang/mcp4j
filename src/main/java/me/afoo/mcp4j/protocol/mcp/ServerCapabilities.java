package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP Server capabilities.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerCapabilities {
    
    @JsonProperty("tools")
    private ToolsCapability tools;

    public ServerCapabilities() {
    }

    public ServerCapabilities(ToolsCapability tools) {
        this.tools = tools;
    }

    public ToolsCapability getTools() {
        return tools;
    }

    public void setTools(ToolsCapability tools) {
        this.tools = tools;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class ToolsCapability {
        // Empty object for now, can be extended with tool-specific capabilities
        
        public ToolsCapability() {
        }
        
        // Dummy method to make Jackson happy with empty bean
        public boolean isEmpty() {
            return true;
        }
    }
}
