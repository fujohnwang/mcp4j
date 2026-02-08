package me.afoo.mcp4j.server;

import com.sun.net.httpserver.HttpServer;
import me.afoo.mcp4j.tool.Tool;
import me.afoo.mcp4j.tool.ToolRegistry;
import me.afoo.mcp4j.transport.SessionManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Main MCP server implementation using HttpServer.
 */
public class McpServer {
    private final McpServerConfig config;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;
    private HttpServer httpServer;
    private ThreadPoolExecutor executor;
    private volatile boolean running = false;

    private McpServer(Builder builder) {
        this.config = builder.config;
        this.toolRegistry = builder.toolRegistry;
        this.sessionManager = new SessionManager(config.getSessionTimeout().toMillis());
    }

    public synchronized void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        httpServer = HttpServer.create(address, config.getBacklog());
        
        // Create thread pool for handling requests
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        httpServer.setExecutor(executor);
        
        // Register MCP endpoint handler
        McpHttpHandler handler = new McpHttpHandler(config, toolRegistry, sessionManager);
        httpServer.createContext(config.getEndpoint(), handler);
        
        httpServer.start();
        running = true;
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }

        if (httpServer != null) {
            httpServer.stop(0);
        }
        
        if (executor != null) {
            executor.shutdown();
        }
        
        sessionManager.shutdown();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public String getAddress() {
        if (httpServer != null) {
            InetSocketAddress addr = httpServer.getAddress();
            return "http://" + addr.getHostString() + ":" + addr.getPort() + config.getEndpoint();
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private McpServerConfig config = McpServerConfig.builder().build();
        private final ToolRegistry toolRegistry = new ToolRegistry();

        public Builder host(String host) {
            this.config = McpServerConfig.builder()
                .host(host)
                .port(config.getPort())
                .endpoint(config.getEndpoint())
                .sessionTimeout(config.getSessionTimeout())
                .serverName(config.getServerName())
                .serverVersion(config.getServerVersion())
                .backlog(config.getBacklog())
                .build();
            return this;
        }

        public Builder port(int port) {
            this.config = McpServerConfig.builder()
                .host(config.getHost())
                .port(port)
                .endpoint(config.getEndpoint())
                .sessionTimeout(config.getSessionTimeout())
                .serverName(config.getServerName())
                .serverVersion(config.getServerVersion())
                .backlog(config.getBacklog())
                .build();
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.config = McpServerConfig.builder()
                .host(config.getHost())
                .port(config.getPort())
                .endpoint(endpoint)
                .sessionTimeout(config.getSessionTimeout())
                .serverName(config.getServerName())
                .serverVersion(config.getServerVersion())
                .backlog(config.getBacklog())
                .build();
            return this;
        }

        public Builder sessionTimeout(Duration sessionTimeout) {
            this.config = McpServerConfig.builder()
                .host(config.getHost())
                .port(config.getPort())
                .endpoint(config.getEndpoint())
                .sessionTimeout(sessionTimeout)
                .serverName(config.getServerName())
                .serverVersion(config.getServerVersion())
                .backlog(config.getBacklog())
                .build();
            return this;
        }

        public Builder serverName(String serverName) {
            this.config = McpServerConfig.builder()
                .host(config.getHost())
                .port(config.getPort())
                .endpoint(config.getEndpoint())
                .sessionTimeout(config.getSessionTimeout())
                .serverName(serverName)
                .serverVersion(config.getServerVersion())
                .backlog(config.getBacklog())
                .build();
            return this;
        }

        public Builder serverVersion(String serverVersion) {
            this.config = McpServerConfig.builder()
                .host(config.getHost())
                .port(config.getPort())
                .endpoint(config.getEndpoint())
                .sessionTimeout(config.getSessionTimeout())
                .serverName(config.getServerName())
                .serverVersion(serverVersion)
                .backlog(config.getBacklog())
                .build();
            return this;
        }

        public Builder tool(Tool tool) {
            toolRegistry.register(tool);
            return this;
        }

        public McpServer build() {
            if (toolRegistry.getAllTools().isEmpty()) {
                throw new IllegalStateException("At least one tool must be registered");
            }
            return new McpServer(this);
        }
    }
}
