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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("text")
        private String text;
        
        @JsonProperty("data")
        private String data;
        
        @JsonProperty("mimeType")
        private String mimeType;
        
        @JsonProperty("resource")
        private Object resource;

        public Content() {}

        public static Content text(String text) {
            Content c = new Content();
            c.type = "text";
            c.text = text;
            return c;
        }

        public static Content image(String data, String mimeType) {
            Content c = new Content();
            c.type = "image";
            c.data = data;
            c.mimeType = mimeType;
            return c;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String getData() {
            return data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Object getResource() {
            return resource;
        }
    }
}
