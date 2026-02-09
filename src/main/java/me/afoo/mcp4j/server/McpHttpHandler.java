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
import me.afoo.mcp4j.transport.SseWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP handler for MCP protocol requests.
 */
public class McpHttpHandler implements HttpHandler {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SESSION_ID_PARAM = "sessionId";
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
        
        // Set CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, " + PROTOCOL_VERSION_HEADER);
        
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if ("POST".equals(method)) {
                handlePost(exchange);
            } else if ("GET".equals(method)) {
                handleGet(exchange);
            } else if ("DELETE".equals(method)) {
                handleDelete(exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String sessionId = null;
        if (query != null && query.contains(SESSION_ID_PARAM + "=")) {
            sessionId = query.split(SESSION_ID_PARAM + "=")[1].split("&")[0];
        }
        
        if (sessionId == null || !sessionManager.isValidSession(sessionId)) {
            sendHttpError(exchange, 404, "Session not found");
            return;
        }

        // Read request body
        InputStream is = exchange.getRequestBody();
        byte[] requestBytes = readAllBytes(is);
        String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
        
        JsonRpcRequest request;
        try {
            request = MAPPER.readValue(requestBody, JsonRpcRequest.class);
        } catch (Exception e) {
            JsonRpcResponse errorResponse = new JsonRpcResponse(null, new JsonRpcError(JsonRpcError.PARSE_ERROR, "Parse error"));
            sendToSse(sessionId, errorResponse);
            exchange.sendResponseHeaders(202, -1);
            return;
        }

        // Process request
        JsonRpcResponse response = processRequest(request, sessionId);
        
        if (response != null) {
            sendToSse(sessionId, response);
        }

        // Standard MCP SSE: POST returns 202 Accepted
        exchange.sendResponseHeaders(202, -1);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        // SSE handshake
        String sessionId = sessionManager.createSession();
        SseWriter writer = new SseWriter(exchange);
        sessionManager.setSseWriter(sessionId, writer);

        // Send endpoint event
        String endpointUrl = config.getEndpoint() + "?" + SESSION_ID_PARAM + "=" + sessionId;
        writer.writeEvent("endpoint", endpointUrl);
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains(SESSION_ID_PARAM + "=")) {
            String sessionId = query.split(SESSION_ID_PARAM + "=")[1].split("&")[0];
            sessionManager.removeSession(sessionId);
        }
        exchange.sendResponseHeaders(204, -1);
    }

    private void sendToSse(String sessionId, JsonRpcResponse response) {
        SseWriter writer = sessionManager.getSseWriter(sessionId);
        if (writer != null) {
            try {
                writer.writeEvent("message", response);
            } catch (IOException e) {
                sessionManager.removeSession(sessionId);
            }
        }
    }

    private JsonRpcResponse processRequest(JsonRpcRequest request, String sessionId) {
        String method = request.getMethod();
        Object id = request.getId();

        try {
            switch (method) {
                case "initialize":
                    return handleInitialize(id, request.getParams());
                case "notifications/initialized":
                case "initialized":
                    // Notification, no response
                    return null;
                case "tools/list":
                    return handleToolsList(id);
                case "tools/call":
                    return handleToolsCall(id, request.getParams());
                case "ping":
                    return new JsonRpcResponse(id, new java.util.HashMap<>());
                default:
                    if (id == null) return null; // Notification
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
            InitializeResult.Implementation serverInfo = 
                new InitializeResult.Implementation(config.getServerName(), config.getServerVersion());
            
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
            // Even if result.getIsError() is true, it's still a successful JSON-RPC response in MCP sense
            return new JsonRpcResponse(id, result);
        } catch (Exception e) {
            return new JsonRpcResponse(id, 
                new JsonRpcError(JsonRpcError.INVALID_PARAMS, "Invalid tool call params"));
        }
    }

    private void sendJsonResponse(HttpExchange exchange, Object response) throws IOException {
        String json = MAPPER.writeValueAsString(response);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void sendJsonRpcError(HttpExchange exchange, Object id, int code, String message) throws IOException {
        JsonRpcResponse response = new JsonRpcResponse(id, new JsonRpcError(code, message));
        sendJsonResponse(exchange, response);
    }

    private void sendHttpError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendHttpError(exchange, statusCode, message);
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
}
