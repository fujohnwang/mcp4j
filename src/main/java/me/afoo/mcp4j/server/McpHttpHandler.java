package me.afoo.mcp4j.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.afoo.mcp4j.protocol.jsonrpc.JsonRpcError;
import me.afoo.mcp4j.protocol.jsonrpc.JsonRpcRequest;
import me.afoo.mcp4j.protocol.jsonrpc.JsonRpcResponse;
import me.afoo.mcp4j.protocol.mcp.*;
import me.afoo.mcp4j.tool.Tool;
import me.afoo.mcp4j.tool.ToolExecutor;
import me.afoo.mcp4j.tool.ToolRegistry;
import me.afoo.mcp4j.transport.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP handler implementing MCP Streamable HTTP transport (2025-03-26).
 *
 * <ul>
 *   <li>POST: Client sends JSON-RPC messages; server responds with application/json</li>
 *   <li>GET: Returns 405 (server-initiated SSE not supported)</li>
 *   <li>DELETE: Terminates session identified by Mcp-Session-Id header</li>
 * </ul>
 */
public class McpHttpHandler implements HttpHandler {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROTOCOL_VERSION = "2025-03-26";
    private static final String SESSION_HEADER = "Mcp-Session-Id";
    private static final String PROTOCOL_VERSION_HEADER = "MCP-Protocol-Version";

    private final McpServerConfig config;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final SessionManager sessionManager;

    public McpHttpHandler(McpServerConfig config, ToolRegistry toolRegistry, SessionManager sessionManager) {
        this.config = config;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = new ToolExecutor(toolRegistry);
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, Accept, " + SESSION_HEADER + ", " + PROTOCOL_VERSION_HEADER);
        exchange.getResponseHeaders().set("Access-Control-Expose-Headers", SESSION_HEADER);

        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (method) {
                case "POST":
                    handlePost(exchange);
                    break;
                case "GET":
                    // Streamable HTTP: server MAY return 405 if it doesn't offer SSE
                    sendHttpError(exchange, 405, "Method Not Allowed");
                    break;
                case "DELETE":
                    handleDelete(exchange);
                    break;
                default:
                    sendHttpError(exchange, 405, "Method Not Allowed");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendHttpError(exchange, 500, "Internal server error");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        // Parse request body
        byte[] requestBytes = readAllBytes(exchange.getRequestBody());
        String requestBody = new String(requestBytes, StandardCharsets.UTF_8);

        JsonRpcRequest request;
        try {
            request = MAPPER.readValue(requestBody, JsonRpcRequest.class);
        } catch (Exception e) {
            JsonRpcResponse errorResp = new JsonRpcResponse(null,
                    new JsonRpcError(JsonRpcError.PARSE_ERROR, "Parse error"));
            sendJsonResponse(exchange, 200, errorResp);
            return;
        }

        // Initialize is special: no session required, creates one
        if ("initialize".equals(request.getMethod())) {
            handleInitializePost(exchange, request);
            return;
        }

        // All other requests require a valid session
        String sessionId = exchange.getRequestHeaders().getFirst(SESSION_HEADER);
        if (sessionId == null || !sessionManager.isValidSession(sessionId)) {
            // No valid session: 400 if missing, 404 if expired/unknown
            if (sessionId == null) {
                sendHttpError(exchange, 400, "Missing Mcp-Session-Id header");
            } else {
                sendHttpError(exchange, 404, "Session not found or expired");
            }
            return;
        }

        // Validate MCP-Protocol-Version header
        String protocolVersion = exchange.getRequestHeaders().getFirst(PROTOCOL_VERSION_HEADER);
        if (protocolVersion != null && !PROTOCOL_VERSION.equals(protocolVersion)) {
            sendHttpError(exchange, 400, "Unsupported MCP-Protocol-Version: " + protocolVersion);
            return;
        }

        // Process the request
        JsonRpcResponse response = processRequest(request);

        if (response == null) {
            // Notification — return 202 Accepted with no body
            exchange.sendResponseHeaders(202, -1);
        } else {
            sendJsonResponse(exchange, 200, response);
        }
    }

    private void handleInitializePost(HttpExchange exchange, JsonRpcRequest request) throws IOException {
        JsonRpcResponse response = handleInitialize(request.getId(), request.getParams());

        // Create session and include Mcp-Session-Id in response header
        String sessionId = sessionManager.createSession();
        exchange.getResponseHeaders().set(SESSION_HEADER, sessionId);

        sendJsonResponse(exchange, 200, response);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String sessionId = exchange.getRequestHeaders().getFirst(SESSION_HEADER);
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        exchange.sendResponseHeaders(204, -1);
    }

    // --- MCP protocol handlers ---

    private JsonRpcResponse processRequest(JsonRpcRequest request) {
        String method = request.getMethod();
        Object id = request.getId();

        try {
            switch (method) {
                case "notifications/initialized":
                    return null; // Notification, no response
                case "tools/list":
                    return handleToolsList(id);
                case "tools/call":
                    return handleToolsCall(id, request.getParams());
                case "ping":
                    return new JsonRpcResponse(id, Collections.emptyMap());
                default:
                    if (id == null) return null; // Unknown notification
                    return new JsonRpcResponse(id,
                            new JsonRpcError(JsonRpcError.METHOD_NOT_FOUND, "Method not found: " + method));
            }
        } catch (Exception e) {
            if (id == null) return null;
            return new JsonRpcResponse(id,
                    new JsonRpcError(JsonRpcError.INTERNAL_ERROR, "Internal error: " + e.getMessage()));
        }
    }

    private JsonRpcResponse handleInitialize(Object id, Object params) {
        try {
            InitializeRequest initRequest = MAPPER.convertValue(params, InitializeRequest.class);

            ServerCapabilities capabilities = new ServerCapabilities(new ServerCapabilities.ToolsCapability());
            Implementation serverInfo = new Implementation(config.getServerName(), config.getServerVersion());

            InitializeResult result = new InitializeResult(PROTOCOL_VERSION, capabilities, serverInfo);
            return new JsonRpcResponse(id, result);
        } catch (Exception e) {
            return new JsonRpcResponse(id,
                    new JsonRpcError(JsonRpcError.INVALID_PARAMS, "Invalid initialize params"));
        }
    }

    private JsonRpcResponse handleToolsList(Object id) {
        List<Tool> tools = toolRegistry.getAllTools();
        List<ToolInfo> toolInfos = tools.stream()
                .map(tool -> new ToolInfo(tool.getName(), tool.getDescription(), tool.getInputSchema()))
                .collect(Collectors.toList());

        ToolsListResult result = new ToolsListResult(toolInfos);
        return new JsonRpcResponse(id, result);
    }

    private JsonRpcResponse handleToolsCall(Object id, Object params) {
        try {
            ToolsCallRequest callRequest = MAPPER.convertValue(params, ToolsCallRequest.class);
            ToolsCallResult result = toolExecutor.execute(callRequest.getName(), callRequest.getArguments());
            return new JsonRpcResponse(id, result);
        } catch (Exception e) {
            return new JsonRpcResponse(id,
                    new JsonRpcError(JsonRpcError.INVALID_PARAMS, "Invalid tool call params"));
        }
    }

    // --- HTTP helpers ---

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object response) throws IOException {
        String json = MAPPER.writeValueAsString(response);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendHttpError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
}
