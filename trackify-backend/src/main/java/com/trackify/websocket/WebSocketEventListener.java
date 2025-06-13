package com.trackify.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.trackify.dto.websocket.NotificationMessage;
import com.trackify.dto.websocket.WebSocketResponse;

import java.time.LocalDateTime;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Anonymous";
        
        logger.info("Received a new web socket connection for user: {} with session: {}", username, sessionId);
        
        if (username != null && !username.equals("Anonymous")) {
            sessionManager.addUserSession(username, sessionId);
            
            // Send welcome notification
            NotificationMessage welcomeMessage = new NotificationMessage(
                "Welcome to Trackify!",
                "You are now connected to real-time updates.",
                "INFO",
                LocalDateTime.now()
            );
            
            WebSocketResponse response = new WebSocketResponse(
                "NOTIFICATION",
                welcomeMessage,
                LocalDateTime.now(),
                true
            );
            
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", response);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Anonymous";
        
        logger.info("User {} disconnected with session: {}", username, sessionId);
        
        if (username != null && !username.equals("Anonymous")) {
            sessionManager.removeUserSession(username, sessionId);
        }
    }
}