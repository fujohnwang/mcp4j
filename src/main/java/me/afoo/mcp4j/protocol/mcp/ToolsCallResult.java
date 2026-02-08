package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP tools/call response result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolsCallResult {
    
    @JsonProperty("content")
    private List<Content> content;
    
    @JsonProperty("isError")
    private Boolean isError;

    public ToolsCallResult() {
        this.content = new ArrayList<>();
    }

    public ToolsCallResult(List<Content> content) {
        this.content = content;
    }

    public List<Content> getContent() {
        return content;
    }

    public void setContent(List<Content> content) {
        this.content = content;
    }

    public Boolean getIsError() {
        return isError;
    }

    public void setIsError(Boolean isError) {
        this.isError = isError;
    }

    public static class Content {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("text")
        private String text;

        public Content(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }
}
