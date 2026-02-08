package me.afoo.mcp4j.transport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages MCP sessions with timeout support.
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

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, System.currentTimeMillis());
        sessions.put(sessionId, session);
        return sessionId;
    }

    public boolean isValidSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        if (now - session.getLastAccessTime() > sessionTimeoutMillis) {
            sessions.remove(sessionId);
            return false;
        }
        
        session.updateLastAccessTime(now);
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
        private long lastAccessTime;

        public Session(String id, long lastAccessTime) {
            this.id = id;
            this.lastAccessTime = lastAccessTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateLastAccessTime(long time) {
            this.lastAccessTime = time;
        }
    }
}
