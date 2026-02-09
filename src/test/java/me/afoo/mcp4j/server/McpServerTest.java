package me.afoo.mcp4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.afoo.mcp4j.tool.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class McpServerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private McpServer server;
    private int port = 8081;

    @BeforeEach
    void setUp() throws Exception {
        server = McpServer.builder()
                .port(port)
                .serverName("test-server")
                .tool(Tool.builder()
                        .name("test_tool")
                        .description("A test tool")
                        .handler(params -> "success")
                        .build())
                .build();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void testInitialize() throws Exception {
        String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-03-26\","
                + "\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0\"}}}";

        HttpURLConnection conn = postJson("/mcp", initRequest, null);

        assertEquals(200, conn.getResponseCode());
        assertEquals("application/json", conn.getContentType());

        // Verify Mcp-Session-Id header is returned
        String sessionId = conn.getHeaderField("Mcp-Session-Id");
        assertNotNull(sessionId, "Mcp-Session-Id header must be present in initialize response");
        assertFalse(sessionId.isEmpty());

        // Verify response body
        JsonNode response = MAPPER.readTree(conn.getInputStream());
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(1, response.get("id").asInt());
        assertNotNull(response.get("result"));
        assertEquals("2025-03-26", response.get("result").get("protocolVersion").asText());
        assertNotNull(response.get("result").get("capabilities"));
        assertNotNull(response.get("result").get("serverInfo"));

        conn.disconnect();
    }

    @Test
    void testToolsList() throws Exception {
        // First initialize to get session
        String sessionId = initialize();

        // List tools
        String listRequest = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}";
        HttpURLConnection conn = postJson("/mcp", listRequest, sessionId);

        assertEquals(200, conn.getResponseCode());
        JsonNode response = MAPPER.readTree(conn.getInputStream());
        assertEquals(2, response.get("id").asInt());

        JsonNode tools = response.get("result").get("tools");
        assertNotNull(tools);
        assertTrue(tools.isArray());
        assertEquals(1, tools.size());
        assertEquals("test_tool", tools.get(0).get("name").asText());

        conn.disconnect();
    }

    @Test
    void testToolCall() throws Exception {
        String sessionId = initialize();

        String callRequest = "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"test_tool\",\"arguments\":{}}}";
        HttpURLConnection conn = postJson("/mcp", callRequest, sessionId);

        assertEquals(200, conn.getResponseCode());
        JsonNode response = MAPPER.readTree(conn.getInputStream());
        assertEquals(3, response.get("id").asInt());

        JsonNode content = response.get("result").get("content").get(0);
        assertEquals("text", content.get("type").asText());
        assertEquals("success", content.get("text").asText());

        conn.disconnect();
    }

    @Test
    void testNotificationReturns202() throws Exception {
        String sessionId = initialize();

        String notification = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
        HttpURLConnection conn = postJson("/mcp", notification, sessionId);

        assertEquals(202, conn.getResponseCode());
        conn.disconnect();
    }

    @Test
    void testMissingSessionReturns400() throws Exception {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}";
        HttpURLConnection conn = postJson("/mcp", request, null);

        assertEquals(400, conn.getResponseCode());
        conn.disconnect();
    }

    @Test
    void testInvalidSessionReturns404() throws Exception {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}";
        HttpURLConnection conn = postJson("/mcp", request, "invalid-session-id");

        assertEquals(404, conn.getResponseCode());
        conn.disconnect();
    }

    @Test
    void testDeleteSession() throws Exception {
        String sessionId = initialize();

        // DELETE to terminate session
        URL url = new URL("http://localhost:" + port + "/mcp");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Mcp-Session-Id", sessionId);

        assertEquals(204, conn.getResponseCode());
        conn.disconnect();

        // Subsequent request with that session should fail
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}";
        HttpURLConnection conn2 = postJson("/mcp", request, sessionId);
        assertEquals(404, conn2.getResponseCode());
        conn2.disconnect();
    }

    @Test
    void testGetReturns405() throws Exception {
        URL url = new URL("http://localhost:" + port + "/mcp");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(405, conn.getResponseCode());
        conn.disconnect();
    }

    @Test
    void testPing() throws Exception {
        String sessionId = initialize();

        String pingRequest = "{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"ping\"}";
        HttpURLConnection conn = postJson("/mcp", pingRequest, sessionId);

        assertEquals(200, conn.getResponseCode());
        JsonNode response = MAPPER.readTree(conn.getInputStream());
        assertEquals(10, response.get("id").asInt());
        assertNotNull(response.get("result"));

        conn.disconnect();
    }

    // --- helpers ---

    private String initialize() throws Exception {
        String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                + "\"params\":{\"protocolVersion\":\"2025-03-26\","
                + "\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0\"}}}";
        HttpURLConnection conn = postJson("/mcp", initRequest, null);
        assertEquals(200, conn.getResponseCode());
        String sessionId = conn.getHeaderField("Mcp-Session-Id");
        assertNotNull(sessionId);
        conn.getInputStream().close();
        conn.disconnect();
        return sessionId;
    }

    private HttpURLConnection postJson(String path, String body, String sessionId) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json, text/event-stream");
        if (sessionId != null) {
            conn.setRequestProperty("Mcp-Session-Id", sessionId);
        }
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }
}
