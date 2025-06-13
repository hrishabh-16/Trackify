package com.trackify.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    // Map to store user sessions: username -> set of session IDs
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    // Map to store session to user mapping: sessionId -> username
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    public void addUserSession(String username, String sessionId) {
        userSessions.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        sessionToUser.put(sessionId, username);
        logger.info("Added session {} for user {}", sessionId, username);
        logger.debug("Total active sessions for user {}: {}", username, userSessions.get(username).size());
    }

    public void removeUserSession(String username, String sessionId) {
        Set<String> sessions = userSessions.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(username);
            }
        }
        sessionToUser.remove(sessionId);
        logger.info("Removed session {} for user {}", sessionId, username);
    }

    public Set<String> getUserSessions(String username) {
        return userSessions.getOrDefault(username, new CopyOnWriteArraySet<>());
    }

    public String getUserBySession(String sessionId) {
        return sessionToUser.get(sessionId);
    }

    public boolean isUserOnline(String username) {
        Set<String> sessions = userSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<String> getOnlineUsers() {
        return userSessions.keySet();
    }

    public int getTotalActiveSessions() {
        return sessionToUser.size();
    }

    public int getUserSessionCount(String username) {
        Set<String> sessions = userSessions.get(username);
        return sessions != null ? sessions.size() : 0;
    }

    public void removeAllUserSessions(String username) {
        Set<String> sessions = userSessions.remove(username);
        if (sessions != null) {
            sessions.forEach(sessionToUser::remove);
            logger.info("Removed all sessions for user {}", username);
        }
    }

    public Map<String, Integer> getUserSessionCounts() {
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        userSessions.forEach((username, sessions) -> counts.put(username, sessions.size()));
        return counts;
    }
}