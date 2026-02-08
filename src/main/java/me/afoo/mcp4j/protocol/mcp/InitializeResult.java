package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP Initialize response result.
 */
public class InitializeResult {
    
    @JsonProperty("protocolVersion")
    private String protocolVersion;
    
    @JsonProperty("capabilities")
    private ServerCapabilities capabilities;
    
    @JsonProperty("serverInfo")
    private Implementation serverInfo;

    public InitializeResult(String protocolVersion, ServerCapabilities capabilities, Implementation serverInfo) {
        this.protocolVersion = protocolVersion;
        this.capabilities = capabilities;
        this.serverInfo = serverInfo;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    public Implementation getServerInfo() {
        return serverInfo;
    }

    public static class Implementation {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("version")
        private String version;

        public Implementation(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }
}
