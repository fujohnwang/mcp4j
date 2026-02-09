package me.afoo.mcp4j.transport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages MCP sessions with timeout support.
 * Sessions are identified by Mcp-Session-Id header per Streamable HTTP transport spec.
 */
public class SessionManager {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final long sessionTimeoutMillis;
    private final ScheduledExecutorService cleanupExecutor;

    public SessionManager(long sessionTimeoutMillis) {
        this.sessionTimeoutMillis = sessionTimeoutMillis;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        startCleanupTask();
    }

    /**
     * Create a new session and return its ID.
     * The ID is a UUID suitable for use as Mcp-Session-Id header value.
     */
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new Session(sessionId, System.currentTimeMillis()));
        return sessionId;
    }

    public boolean isValidSession(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        Session session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - session.getLastAccessTime() > sessionTimeoutMillis) {
            sessions.remove(sessionId);
            return false;
        }

        session.touch(now);
        return true;
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        sessions.clear();
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            sessions.entrySet().removeIf(entry ->
                now - entry.getValue().getLastAccessTime() > sessionTimeoutMillis
            );
        }, 1, 1, TimeUnit.MINUTES);
    }

    private static class Session {
        private final String id;
        private volatile long lastAccessTime;

        Session(String id, long lastAccessTime) {
            this.id = id;
            this.lastAccessTime = lastAccessTime;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }

        void touch(long time) {
            this.lastAccessTime = time;
        }
    }
}
