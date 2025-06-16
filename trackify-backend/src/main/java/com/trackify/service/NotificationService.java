package com.trackify.service;

import com.trackify.entity.Notification;
import com.trackify.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    // Create notifications
    Notification createNotification(Long userId, NotificationType type, String title, String message);
    Notification createNotification(Long userId, NotificationType type, String title, String message, 
                                   String entityType, Long entityId);
    Notification createNotification(Long userId, NotificationType type, String title, String message,
                                   String entityType, Long entityId, String priority);

    Notification createNotification(Long userId, NotificationType type, String title, String message, String priority);
    
    // Bulk notification creation
    List<Notification> createBulkNotifications(List<Long> userIds, NotificationType type, 
                                              String title, String message);
    List<Notification> createBulkNotifications(List<Long> userIds, NotificationType type, 
                                              String title, String message, String entityType, Long entityId);

    // Expense-related notifications
    void notifyExpenseSubmitted(Long expenseId, Long submitterId, Long approverId);
    void notifyExpenseApproved(Long expenseId, Long submitterId, Long approverId);
    void notifyExpenseRejected(Long expenseId, Long submitterId, Long approverId, String reason);
    void notifyExpenseEscalated(Long expenseId, Long submitterId, Long newApproverId);
    void notifyExpenseOverdue(Long expenseId, Long submitterId, Long approverId);

    // Budget-related notifications
    void notifyBudgetExceeded(Long budgetId, Long userId, String budgetName, Double percentage);
    void notifyBudgetWarning(Long budgetId, Long userId, String budgetName, Double percentage);
    void notifyBudgetExpired(Long budgetId, List<Long> userIds, String budgetName);

    // Comment-related notifications
    void notifyCommentAdded(Long commentId, Long expenseId, Long authorId, List<Long> recipientIds);
    void notifyCommentReply(Long commentId, Long parentCommentId, Long authorId, Long recipientId);
    void notifyCommentMention(Long commentId, Long authorId, List<Long> mentionedUserIds);

    // Approval-related notifications
    void notifyApprovalRequest(Long workflowId, Long expenseId, Long submitterId, Long approverId);
    void notifyApprovalReminder(Long workflowId, Long approverId);
    void notifyApprovalEscalation(Long workflowId, Long fromApproverId, Long toApproverId);

    // System notifications
    void notifySystemUpdate(List<Long> userIds, String title, String message);
    void notifyReportGenerated(Long userId, String reportName, String downloadUrl);
    void notifyPolicyViolation(Long userId, Long expenseId, String violation);
    void notifyDuplicateExpense(Long userId, Long expenseId, Long duplicateExpenseId);

    // Security notifications
    void notifyAccountLocked(Long userId);
    void notifyPasswordReset(Long userId);
    void notifyLoginAlert(Long userId, String location, String device);

    // AI-related notifications
    void notifyAiSuggestion(Long userId, String suggestion, String entityType, Long entityId);
    void notifyAnomalyDetected(Long userId, String anomaly, String entityType, Long entityId);

    // Retrieve notifications
    List<Notification> getUserNotifications(Long userId);
    Page<Notification> getUserNotifications(Long userId, Pageable pageable);
    List<Notification> getUnreadNotifications(Long userId);
    Page<Notification> getUnreadNotifications(Long userId, Pageable pageable);
    List<Notification> getNotificationsByType(Long userId, NotificationType type);
    List<Notification> getNotificationsByCategory(Long userId, String category);
    List<Notification> getNotificationsByPriority(Long userId, String priority);
    List<Notification> getRecentNotifications(Long userId, int hours);

    // Notification management
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    void markCategoryAsRead(Long userId, String category);
    void markAsReadByIds(List<Long> notificationIds, Long userId);
    void deleteNotification(Long notificationId, Long userId);
    void deleteOldNotifications(Long userId, int daysOld);

    // Notification counts
    long getUnreadCount(Long userId);
    long getUnreadCountByType(Long userId, NotificationType type);
    long getUnreadCountByCategory(Long userId, String category);
    Map<String, Long> getNotificationCounts(Long userId);

    // Notification settings and preferences
    void updateNotificationPreferences(Long userId, Map<String, Boolean> preferences);
    Map<String, Boolean> getNotificationPreferences(Long userId);
    boolean isNotificationEnabled(Long userId, NotificationType type);

    // Email and push notifications
    void sendEmailNotification(Long notificationId);
    void sendPushNotification(Long notificationId);
    void sendBulkEmailNotifications(List<Long> notificationIds);
    void sendBulkPushNotifications(List<Long> notificationIds);

    // Notification delivery
    void processNotificationQueue();
    void retryFailedNotifications();
    void markEmailSent(Long notificationId);
    void markPushSent(Long notificationId);

    // Notification grouping
    void groupSimilarNotifications(Long userId);
    List<Notification> getGroupedNotifications(Long userId, String groupKey);

    // Statistics and analytics
    Map<String, Object> getNotificationStatistics(Long userId);
    Map<String, Object> getNotificationStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    List<Map<String, Object>> getNotificationTrendData(Long userId, int days);

    // Administrative functions
    void cleanupExpiredNotifications();
    void cleanupOldReadNotifications(int daysOld);
    List<Notification> findNotificationsForRetry();
    void updateRetryCount(Long notificationId);

    // Search and filtering
    Page<Notification> searchNotifications(Long userId, String query, Pageable pageable);
    Page<Notification> filterNotifications(Long userId, NotificationType type, String category, 
                                          String priority, Boolean isRead, LocalDateTime startDate, 
                                          LocalDateTime endDate, Pageable pageable);

    // Batch operations
    void deleteMultipleNotifications(List<Long> notificationIds, Long userId);
    void markMultipleAsRead(List<Long> notificationIds, Long userId);

    // Real-time notifications
    void sendRealTimeNotification(Long userId, Notification notification);
    void broadcastNotification(List<Long> userIds, Notification notification);

    // Template-based notifications
    Notification createFromTemplate(String templateName, Long userId, Map<String, Object> variables);
    void registerNotificationTemplate(String templateName, String titleTemplate, String messageTemplate);

    // Notification validation
    boolean validateNotification(Notification notification);
    boolean canReceiveNotification(Long userId, NotificationType type);

    // Helper methods
    String generateGroupKey(NotificationType type, String entityType, Long entityId);
    void scheduleNotification(Notification notification, LocalDateTime scheduledTime);
    void cancelScheduledNotification(Long notificationId);
}