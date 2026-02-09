package me.afoo.mcp4j.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes Server-Sent Events (SSE) to HTTP response stream.
 */
public class SseWriter {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final OutputStream outputStream;
    private boolean closed = false;

    public SseWriter(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        this.outputStream = exchange.getResponseBody();
    }

    public synchronized void writeEvent(Object data) throws IOException {
        if (closed) {
            throw new IOException("SSE stream is closed");
        }
        
        String json = MAPPER.writeValueAsString(data);
        String event = "data: " + json + "\n\n";
        outputStream.write(event.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public synchronized void writeEvent(String eventType, Object data) throws IOException {
        if (closed) {
            throw new IOException("SSE stream is closed");
        }
        
        String json = MAPPER.writeValueAsString(data);
        String event = "event: " + eventType + "\ndata: " + json + "\n\n";
        outputStream.write(event.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    public synchronized void close() {
        if (!closed) {
            closed = true;
            try {
                outputStream.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
