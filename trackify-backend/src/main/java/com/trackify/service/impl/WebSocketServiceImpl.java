package com.trackify.service.impl;
import com.trackify.dto.websocket.ExpenseWebSocketMessage;
import com.trackify.dto.websocket.NotificationMessage;
import com.trackify.dto.websocket.WebSocketResponse;
import com.trackify.entity.Team;
import com.trackify.entity.TeamMember;
import com.trackify.repository.TeamRepository;
import com.trackify.repository.TeamMemberRepository;
import com.trackify.service.WebSocketService;
import com.trackify.service.DashboardService;
import com.trackify.websocket.WebSocketSessionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Override
    public void broadcastExpenseUpdate(ExpenseWebSocketMessage message) {
        try {
            WebSocketResponse response = WebSocketResponse.success("EXPENSE_UPDATE", message);
            messagingTemplate.convertAndSend("/topic/expenses", response);
            logger.info("Broadcasted expense update: {}", message.getAction());
        } catch (Exception e) {
            logger.error("Error broadcasting expense update", e);
        }
    }

    @Override
    public void sendExpenseUpdateToUser(String username, ExpenseWebSocketMessage message) {
        try {
            if (isUserOnline(username)) {
                WebSocketResponse response = WebSocketResponse.success("EXPENSE_UPDATE", message);
                messagingTemplate.convertAndSendToUser(username, "/queue/expenses", response);
                logger.info("Sent expense update to user {}: {}", username, message.getAction());
            }
        } catch (Exception e) {
            logger.error("Error sending expense update to user {}", username, e);
        }
    }

    @Override
    public void sendExpenseUpdateToTeam(Long teamId, ExpenseWebSocketMessage message) {
        try {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
            
            for (TeamMember member : teamMembers) {
                String username = member.getUser().getUsername();
                sendExpenseUpdateToUser(username, message);
            }
            
            logger.info("Sent expense update to team {} members", teamId);
        } catch (Exception e) {
            logger.error("Error sending expense update to team {}", teamId, e);
        }
    }

    @Override
    public void sendNotificationToUser(String username, NotificationMessage notification) {
        try {
            if (isUserOnline(username)) {
                WebSocketResponse response = WebSocketResponse.success("NOTIFICATION", notification);
                messagingTemplate.convertAndSendToUser(username, "/queue/notifications", response);
                logger.info("Sent notification to user {}: {}", username, notification.getTitle());
            }
        } catch (Exception e) {
            logger.error("Error sending notification to user {}", username, e);
        }
    }

    @Override
    public void broadcastNotification(NotificationMessage notification) {
        try {
            WebSocketResponse response = WebSocketResponse.success("NOTIFICATION", notification);
            messagingTemplate.convertAndSend("/topic/notifications", response);
            logger.info("Broadcasted notification: {}", notification.getTitle());
        } catch (Exception e) {
            logger.error("Error broadcasting notification", e);
        }
    }

    @Override
    public void sendNotificationToUsers(List<String> usernames, NotificationMessage notification) {
        for (String username : usernames) {
            sendNotificationToUser(username, notification);
        }
    }

    @Override
    public void refreshDashboard(String username) {
        try {
            if (isUserOnline(username)) {
                // Get fresh dashboard data
                var dashboardData = dashboardService.getDashboardData(username);
                
                WebSocketResponse response = WebSocketResponse.success("DASHBOARD_REFRESH", dashboardData);
                messagingTemplate.convertAndSendToUser(username, "/queue/dashboard", response);
                logger.info("Refreshed dashboard for user {}", username);
            }
        } catch (Exception e) {
            logger.error("Error refreshing dashboard for user {}", username, e);
        }
    }

    @Override
    public void broadcastDashboardUpdate(Object dashboardData) {
        try {
            WebSocketResponse response = WebSocketResponse.success("DASHBOARD_UPDATE", dashboardData);
            messagingTemplate.convertAndSend("/topic/dashboard", response);
            logger.info("Broadcasted dashboard update");
        } catch (Exception e) {
            logger.error("Error broadcasting dashboard update", e);
        }
    }

    @Override
    public void sendDashboardUpdateToUser(String username, Object dashboardData) {
        try {
            if (isUserOnline(username)) {
                WebSocketResponse response = WebSocketResponse.success("DASHBOARD_UPDATE", dashboardData);
                messagingTemplate.convertAndSendToUser(username, "/queue/dashboard", response);
                logger.info("Sent dashboard update to user {}", username);
            }
        } catch (Exception e) {
            logger.error("Error sending dashboard update to user {}", username, e);
        }
    }

    @Override
    public void sendBudgetAlert(String username, String message, String alertType) {
        try {
            NotificationMessage notification = new NotificationMessage(
                "Budget Alert",
                message,
                alertType,
                "HIGH",
                username,
                LocalDateTime.now()
            );
            
            sendNotificationToUser(username, notification);
            logger.info("Sent budget alert to user {}: {}", username, message);
        } catch (Exception e) {
            logger.error("Error sending budget alert to user {}", username, e);
        }
    }

    @Override
    public void broadcastBudgetAlert(String message, String alertType) {
        try {
            NotificationMessage notification = new NotificationMessage(
                "Budget Alert",
                message,
                alertType,
                LocalDateTime.now()
            );
            
            broadcastNotification(notification);
            logger.info("Broadcasted budget alert: {}", message);
        } catch (Exception e) {
            logger.error("Error broadcasting budget alert", e);
        }
    }

    @Override
    public Set<String> getOnlineUsers() {
        return sessionManager.getOnlineUsers();
    }

    @Override
    public boolean isUserOnline(String username) {
        return sessionManager.isUserOnline(username);
    }

    @Override
    public int getActiveSessionCount() {
        return sessionManager.getTotalActiveSessions();
    }

    @Override
    public void sendMessageToUser(String username, String destination, Object message) {
        try {
            if (isUserOnline(username)) {
                messagingTemplate.convertAndSendToUser(username, destination, message);
                logger.debug("Sent message to user {} at destination {}", username, destination);
            }
        } catch (Exception e) {
            logger.error("Error sending message to user {} at destination {}", username, destination, e);
        }
    }

    @Override
    public void broadcastMessage(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
            logger.debug("Broadcasted message to destination {}", destination);
        } catch (Exception e) {
            logger.error("Error broadcasting message to destination {}", destination, e);
        }
    }

    @Override
    public void sendSystemNotification(String username, String title, String message) {
        try {
            NotificationMessage notification = new NotificationMessage(
                title,
                message,
                "INFO",
                username,
                LocalDateTime.now()
            );
            notification.setCategory("SYSTEM");
            
            sendNotificationToUser(username, notification);
            logger.info("Sent system notification to user {}: {}", username, title);
        } catch (Exception e) {
            logger.error("Error sending system notification to user {}", username, e);
        }
    }

    @Override
    public void broadcastSystemNotification(String title, String message) {
        try {
            NotificationMessage notification = new NotificationMessage(
                title,
                message,
                "INFO",
                LocalDateTime.now()
            );
            notification.setCategory("SYSTEM");
            
            broadcastNotification(notification);
            logger.info("Broadcasted system notification: {}", title);
        } catch (Exception e) {
            logger.error("Error broadcasting system notification", e);
        }
    }

    @Override
    public void sendTeamNotification(Long teamId, NotificationMessage notification) {
        try {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
            
            for (TeamMember member : teamMembers) {
                String username = member.getUser().getUsername();
                sendNotificationToUser(username, notification);
            }
            
            logger.info("Sent team notification to team {} members: {}", teamId, notification.getTitle());
        } catch (Exception e) {
            logger.error("Error sending team notification to team {}", teamId, e);
        }
    }

    @Override
    public void sendTeamMessage(Long teamId, String destination, Object message) {
        try {
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(teamId);
            
            for (TeamMember member : teamMembers) {
                String username = member.getUser().getUsername();
                sendMessageToUser(username, destination, message);
            }
            
            logger.info("Sent team message to team {} members at destination {}", teamId, destination);
        } catch (Exception e) {
            logger.error("Error sending team message to team {} at destination {}", teamId, destination, e);
        }
    }
}