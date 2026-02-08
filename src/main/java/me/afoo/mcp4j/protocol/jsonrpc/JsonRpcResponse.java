package me.afoo.mcp4j.protocol.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 Response.
 */
public class JsonRpcResponse extends JsonRpcMessage {
    
    @JsonProperty("id")
    private Object id;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private JsonRpcError error;

    public JsonRpcResponse() {
    }

    public JsonRpcResponse(Object id, Object result) {
        this.id = id;
        this.result = result;
    }

    public JsonRpcResponse(Object id, JsonRpcError error) {
        this.id = id;
        this.error = error;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }
}
