package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP Initialize request parameters.
 */
public class InitializeRequest {

    @JsonProperty("protocolVersion")
    private String protocolVersion;

    @JsonProperty("capabilities")
    private ClientCapabilities capabilities;

    @JsonProperty("clientInfo")
    private Implementation clientInfo;

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public ClientCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public Implementation getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Implementation clientInfo) {
        this.clientInfo = clientInfo;
    }

    public static class ClientCapabilities {
        // Client capabilities can be extended as needed
    }
}
