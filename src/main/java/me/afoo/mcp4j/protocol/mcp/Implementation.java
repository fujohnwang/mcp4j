package me.afoo.mcp4j.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Shared implementation info for both client and server.
 */
public class Implementation {
    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    public Implementation() {
    }

    public Implementation(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
