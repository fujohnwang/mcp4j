package me.afoo.mcp4j.server;

import java.time.Duration;

/**
 * Configuration for MCP server.
 */
public class McpServerConfig {
    private final String host;
    private final int port;
    private final String endpoint;
    private final Duration sessionTimeout;
    private final String serverName;
    private final String serverVersion;
    private final int backlog;

    private McpServerConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.endpoint = builder.endpoint;
        this.sessionTimeout = builder.sessionTimeout;
        this.serverName = builder.serverName;
        this.serverVersion = builder.serverVersion;
        this.backlog = builder.backlog;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Duration getSessionTimeout() {
        return sessionTimeout;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public int getBacklog() {
        return backlog;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String host = "127.0.0.1";
        private int port = 8080;
        private String endpoint = "/mcp";
        private Duration sessionTimeout = Duration.ofMinutes(30);
        private String serverName = "mcp4j-server";
        private String serverVersion = "0.1.0";
        private int backlog = 0;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder sessionTimeout(Duration sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public Builder serverVersion(String serverVersion) {
            this.serverVersion = serverVersion;
            return this;
        }

        public Builder backlog(int backlog) {
            this.backlog = backlog;
            return this;
        }

        public McpServerConfig build() {
            return new McpServerConfig(this);
        }
    }
}
