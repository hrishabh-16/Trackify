package com.trackify.service;

import com.trackify.dto.websocket.ExpenseWebSocketMessage;
import com.trackify.dto.websocket.NotificationMessage;
import com.trackify.dto.websocket.WebSocketResponse;

import java.util.List;
import java.util.Set;

public interface WebSocketService {
    
    // Expense-related WebSocket operations
    void broadcastExpenseUpdate(ExpenseWebSocketMessage message);
    void sendExpenseUpdateToUser(String username, ExpenseWebSocketMessage message);
    void sendExpenseUpdateToTeam(Long teamId, ExpenseWebSocketMessage message);
    
    // Notification operations
    void sendNotificationToUser(String username, NotificationMessage notification);
    void broadcastNotification(NotificationMessage notification);
    void sendNotificationToUsers(List<String> usernames, NotificationMessage notification);
    
    // Dashboard operations
    void refreshDashboard(String username);
    void broadcastDashboardUpdate(Object dashboardData);
    void sendDashboardUpdateToUser(String username, Object dashboardData);
    
    // Budget alert operations
    void sendBudgetAlert(String username, String message, String alertType);
    void broadcastBudgetAlert(String message, String alertType);
    
    // User session operations
    Set<String> getOnlineUsers();
    boolean isUserOnline(String username);
    int getActiveSessionCount();
    
    // Generic message operations
    void sendMessageToUser(String username, String destination, Object message);
    void broadcastMessage(String destination, Object message);
    
    // System notifications
    void sendSystemNotification(String username, String title, String message);
    void broadcastSystemNotification(String title, String message);
    
    // Team-specific operations
    void sendTeamNotification(Long teamId, NotificationMessage notification);
    void sendTeamMessage(Long teamId, String destination, Object message);
}