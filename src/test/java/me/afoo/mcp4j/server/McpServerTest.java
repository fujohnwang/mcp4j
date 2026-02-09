package me.afoo.mcp4j.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.afoo.mcp4j.tool.Tool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    void testSseHandshakeAndInitialize() throws Exception {
        // 1. GET Handshake
        URL url = new URL("http://localhost:" + port + "/mcp");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/event-stream");

        assertEquals(200, conn.getResponseCode());
        assertEquals("text/event-stream", conn.getContentType());

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        
        // 2. Read endpoint event
        String line;
        String endpointLine = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("event: endpoint")) {
                endpointLine = reader.readLine(); // Read next line for data
                break;
            }
        }
        
        assertNotNull(endpointLine);
        assertTrue(endpointLine.startsWith("data: "));
        String postUrlSuffix = MAPPER.readTree(endpointLine.substring(6)).asText();
        String sessionId = postUrlSuffix.split("sessionId=")[1];

        // 3. POST Initialize
        URL postUrl = new URL("http://localhost:" + port + postUrlSuffix);
        HttpURLConnection postConn = (HttpURLConnection) postUrl.openConnection();
        postConn.setRequestMethod("POST");
        postConn.setDoOutput(true);
        postConn.setRequestProperty("Content-Type", "application/json");

        String initRequest = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2024-11-05\",\"clientInfo\":{\"name\":\"test-client\",\"version\":\"1.0\"}}}";
        try (OutputStream os = postConn.getOutputStream()) {
            os.write(initRequest.getBytes());
        }

        assertEquals(202, postConn.getResponseCode());

        // 4. Read response from SSE stream
        String messageLine = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("event: message")) {
                messageLine = reader.readLine();
                break;
            }
        }

        assertNotNull(messageLine);
        assertTrue(messageLine.startsWith("data: "));
        JsonNode response = MAPPER.readTree(messageLine.substring(6));
        
        assertEquals("2.0", response.get("jsonrpc").asText());
        assertEquals(1, response.get("id").asInt());
        assertNotNull(response.get("result"));
        assertEquals("2024-11-05", response.get("result").get("protocolVersion").asText());

        conn.disconnect();
    }

    @Test
    void testToolCall() throws Exception {
        // Setup SSE connection
        URL url = new URL("http://localhost:" + port + "/mcp");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        
        String line;
        String postUrlSuffix = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("event: endpoint")) {
                String dataLine = reader.readLine();
                postUrlSuffix = MAPPER.readTree(dataLine.substring(6)).asText();
                break;
            }
        }

        // POST tool call
        URL postUrl = new URL("http://localhost:" + port + postUrlSuffix);
        HttpURLConnection postConn = (HttpURLConnection) postUrl.openConnection();
        postConn.setRequestMethod("POST");
        postConn.setDoOutput(true);
        postConn.setRequestProperty("Content-Type", "application/json");

        String callRequest = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"test_tool\",\"arguments\":{}}}";
        try (OutputStream os = postConn.getOutputStream()) {
            os.write(callRequest.getBytes());
        }
        assertEquals(202, postConn.getResponseCode());

        // Read result from SSE
        String messageLine = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("event: message")) {
                messageLine = reader.readLine();
                break;
            }
        }

        assertNotNull(messageLine);
        JsonNode response = MAPPER.readTree(messageLine.substring(6));
        assertEquals(2, response.get("id").asInt());
        JsonNode content = response.get("result").get("content").get(0);
        assertEquals("text", content.get("type").asText());
        assertEquals("success", content.get("text").asText());

        conn.disconnect();
    }
}
