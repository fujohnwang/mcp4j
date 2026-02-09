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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolsCapability {

        @JsonProperty("listChanged")
        private Boolean listChanged;

        public ToolsCapability() {
        }

        public ToolsCapability(Boolean listChanged) {
            this.listChanged = listChanged;
        }

        public Boolean getListChanged() {
            return listChanged;
        }

        public void setListChanged(Boolean listChanged) {
            this.listChanged = listChanged;
        }
    }
}
