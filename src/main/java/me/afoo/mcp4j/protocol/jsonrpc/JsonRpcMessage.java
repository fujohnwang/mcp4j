package me.afoo.mcp4j.protocol.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for JSON-RPC 2.0 messages.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class JsonRpcMessage {
    
    @JsonProperty("jsonrpc")
    private final String jsonrpc = "2.0";

    public String getJsonrpc() {
        return jsonrpc;
    }
}
