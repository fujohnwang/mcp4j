package me.afoo.mcp4j.protocol.jsonrpc;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 Request.
 */
public class JsonRpcRequest extends JsonRpcMessage {
    
    @JsonProperty("id")
    private Object id;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private Object params;

    public JsonRpcRequest() {
    }

    public JsonRpcRequest(Object id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
