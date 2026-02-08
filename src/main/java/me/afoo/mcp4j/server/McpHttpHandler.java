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
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP handler for MCP protocol requests.
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
    private boolean initialized = false;

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
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, " + SESSION_HEADER + ", " + PROTOCOL_VERSION_HEADER);
        
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
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String sessionId = exchange.getRequestHeaders().getFirst(SESSION_HEADER);
        
        // Read request body
        InputStream is = exchange.getRequestBody();
        byte[] requestBytes = readAllBytes(is);
        String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
        
        JsonRpcRequest request;
        try {
            request = MAPPER.readValue(requestBody, JsonRpcRequest.class);
        } catch (Exception e) {
            sendJsonRpcError(exchange, null, JsonRpcError.PARSE_ERROR, "Parse error");
            return;
        }

        // Validate session for non-initialize requests
        if (!"initialize".equals(request.getMethod())) {
            if (sessionId == null || !sessionManager.isValidSession(sessionId)) {
                sendHttpError(exchange, 404, "Session not found");
                return;
            }
        }

        // Process request
        JsonRpcResponse response = processRequest(request, sessionId);
        
        // For initialize, create session and add header
        if ("initialize".equals(request.getMethod()) && response.getError() == null) {
            String newSessionId = sessionManager.createSession();
            exchange.getResponseHeaders().set(SESSION_HEADER, newSessionId);
        }

        // Send JSON response
        sendJsonResponse(exchange, response);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        // Optional SSE endpoint for server-initiated messages
        // For now, return 405 as we don't implement server push
        sendError(exchange, 405, "GET not supported");
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String sessionId = exchange.getRequestHeaders().getFirst(SESSION_HEADER);
        
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        
        exchange.sendResponseHeaders(204, -1);
    }

    private JsonRpcResponse processRequest(JsonRpcRequest request, String sessionId) {
        String method = request.getMethod();
        Object id = request.getId();

        try {
            switch (method) {
                case "initialize":
                    return handleInitialize(id, request.getParams());
                case "initialized":
                    initialized = true;
                    return new JsonRpcResponse(id, null);
                case "tools/list":
                    return handleToolsList(id);
                case "tools/call":
                    return handleToolsCall(id, request.getParams());
                default:
                    return new JsonRpcResponse(id, 
                        new JsonRpcError(JsonRpcError.METHOD_NOT_FOUND, "Method not found: " + method));
            }
        } catch (Exception e) {
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
            
            if (Boolean.TRUE.equals(result.getIsError())) {
                return new JsonRpcResponse(id, 
                    new JsonRpcError(JsonRpcError.INTERNAL_ERROR, "Tool execution failed", result));
            }
            
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
