package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP Initialize response result.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitializeResult {

    @JsonProperty("protocolVersion")
    private String protocolVersion;

    @JsonProperty("capabilities")
    private ServerCapabilities capabilities;

    @JsonProperty("serverInfo")
    private Implementation serverInfo;

    @JsonProperty("instructions")
    private String instructions;

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

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
