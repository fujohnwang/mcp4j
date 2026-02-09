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
        this.config = builder.configBuilder.build();
        this.toolRegistry = builder.toolRegistry;
        this.sessionManager = new SessionManager(config.getSessionTimeout().toMillis());
    }

    public synchronized void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        httpServer = HttpServer.create(address, config.getBacklog());

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(config.getThreadPoolSize());
        httpServer.setExecutor(executor);

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
        private final McpServerConfig.Builder configBuilder = McpServerConfig.builder();
        private final ToolRegistry toolRegistry = new ToolRegistry();

        public Builder host(String host) {
            configBuilder.host(host);
            return this;
        }

        public Builder port(int port) {
            configBuilder.port(port);
            return this;
        }

        public Builder endpoint(String endpoint) {
            configBuilder.endpoint(endpoint);
            return this;
        }

        public Builder sessionTimeout(Duration sessionTimeout) {
            configBuilder.sessionTimeout(sessionTimeout);
            return this;
        }

        public Builder serverName(String serverName) {
            configBuilder.serverName(serverName);
            return this;
        }

        public Builder serverVersion(String serverVersion) {
            configBuilder.serverVersion(serverVersion);
            return this;
        }

        public Builder threadPoolSize(int size) {
            configBuilder.threadPoolSize(size);
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
