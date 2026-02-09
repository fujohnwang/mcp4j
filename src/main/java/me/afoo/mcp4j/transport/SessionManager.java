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

    public void setSseWriter(String sessionId, SseWriter writer) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.setSseWriter(writer);
        }
    }

    public SseWriter getSseWriter(String sessionId) {
        Session session = sessions.get(sessionId);
        return (session != null) ? session.getSseWriter() : null;
    }

    public boolean isValidSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        if (now - session.getLastAccessTime() > sessionTimeoutMillis) {
            Session removed = sessions.remove(sessionId);
            if (removed != null && removed.getSseWriter() != null) {
                removed.getSseWriter().close();
            }
            return false;
        }
        
        session.updateLastAccessTime(now);
        return true;
    }

    public void removeSession(String sessionId) {
        Session removed = sessions.remove(sessionId);
        if (removed != null && removed.getSseWriter() != null) {
            removed.getSseWriter().close();
        }
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
        sessions.values().forEach(s -> {
            if (s.getSseWriter() != null) {
                s.getSseWriter().close();
            }
        });
        sessions.clear();
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            sessions.entrySet().removeIf(entry -> {
                boolean expired = now - entry.getValue().getLastAccessTime() > sessionTimeoutMillis;
                if (expired && entry.getValue().getSseWriter() != null) {
                    entry.getValue().getSseWriter().close();
                }
                return expired;
            });
        }, 1, 1, TimeUnit.MINUTES);
    }

    private static class Session {
        private final String id;
        private long lastAccessTime;
        private SseWriter sseWriter;

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

        public SseWriter getSseWriter() {
            return sseWriter;
        }

        public void setSseWriter(SseWriter sseWriter) {
            this.sseWriter = sseWriter;
        }
    }
}
