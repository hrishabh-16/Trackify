package com.trackify.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.dto.websocket.ExpenseWebSocketMessage;
import com.trackify.dto.websocket.NotificationMessage;
import com.trackify.dto.websocket.WebSocketResponse;
import com.trackify.service.WebSocketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MessageMapping("/expense.create")
    @SendTo("/topic/expenses")
    public WebSocketResponse handleExpenseCreate(@Payload ExpenseWebSocketMessage message,
                                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            logger.info("Received expense create message: {}", message);
            
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "Anonymous";
            
            // Process the expense creation
            webSocketService.broadcastExpenseUpdate(message);
            
            return new WebSocketResponse(
                "EXPENSE_CREATED",
                message,
                LocalDateTime.now(),
                true
            );
        } catch (Exception e) {
            logger.error("Error handling expense create message", e);
            return new WebSocketResponse(
                "ERROR",
                "Failed to process expense creation",
                LocalDateTime.now(),
                false
            );
        }
    }

    @MessageMapping("/expense.update")
    @SendTo("/topic/expenses")
    public WebSocketResponse handleExpenseUpdate(@Payload ExpenseWebSocketMessage message,
                                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            logger.info("Received expense update message: {}", message);
            
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "Anonymous";
            
            // Process the expense update
            webSocketService.broadcastExpenseUpdate(message);
            
            return new WebSocketResponse(
                "EXPENSE_UPDATED",
                message,
                LocalDateTime.now(),
                true
            );
        } catch (Exception e) {
            logger.error("Error handling expense update message", e);
            return new WebSocketResponse(
                "ERROR",
                "Failed to process expense update",
                LocalDateTime.now(),
                false
            );
        }
    }

    @MessageMapping("/expense.delete")
    @SendTo("/topic/expenses")
    public WebSocketResponse handleExpenseDelete(@Payload ExpenseWebSocketMessage message,
                                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            logger.info("Received expense delete message: {}", message);
            
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "Anonymous";
            
            // Process the expense deletion
            webSocketService.broadcastExpenseUpdate(message);
            
            return new WebSocketResponse(
                "EXPENSE_DELETED",
                message,
                LocalDateTime.now(),
                true
            );
        } catch (Exception e) {
            logger.error("Error handling expense delete message", e);
            return new WebSocketResponse(
                "ERROR",
                "Failed to process expense deletion",
                LocalDateTime.now(),
                false
            );
        }
    }

    @MessageMapping("/notification.send")
    public void handleNotificationSend(@Payload NotificationMessage message,
                                     SimpMessageHeaderAccessor headerAccessor) {
        try {
            logger.info("Received notification message: {}", message);
            
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "Anonymous";
            
            // Send notification to specific user or broadcast
            webSocketService.sendNotificationToUser(username, message);
            
        } catch (Exception e) {
            logger.error("Error handling notification message", e);
        }
    }

    @MessageMapping("/dashboard.refresh")
    public void handleDashboardRefresh(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = headerAccessor.getUser() != null ? 
                headerAccessor.getUser().getName() : "Anonymous";
            
            logger.info("Received dashboard refresh request from user: {}", username);
            
            // Trigger dashboard data refresh
            webSocketService.refreshDashboard(username);
            
        } catch (Exception e) {
            logger.error("Error handling dashboard refresh", e);
        }
    }
}	