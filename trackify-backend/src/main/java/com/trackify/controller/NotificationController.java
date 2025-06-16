package com.trackify.controller;

import com.trackify.entity.Notification;
import com.trackify.enums.NotificationType;
import com.trackify.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;

    // Get user notifications
    @GetMapping
    public ResponseEntity<Page<Notification>> getUserNotifications(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        
        logger.info("Retrieving notifications for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Page<Notification> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    // Get unread notifications
    @GetMapping("/unread")
    public ResponseEntity<Page<Notification>> getUnreadNotifications(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        
        logger.info("Retrieving unread notifications for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Page<Notification> notifications = notificationService.getUnreadNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    // Get notification by ID
    @GetMapping("/{notificationId}")
    public ResponseEntity<Notification> getNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        // The service will validate ownership
        List<Notification> userNotifications = notificationService.getUserNotifications(userId);
        
        Notification notification = userNotifications.stream()
                .filter(n -> n.getId().equals(notificationId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        return ResponseEntity.ok(notification);
    }

    // Get notifications by type
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByType(
            @PathVariable NotificationType type,
            Authentication authentication) {
        
        logger.info("Retrieving {} notifications for user: {}", type, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        List<Notification> notifications = notificationService.getNotificationsByType(userId, type);
        return ResponseEntity.ok(notifications);
    }

    // Get notifications by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Notification>> getNotificationsByCategory(
            @PathVariable String category,
            Authentication authentication) {
        
        logger.info("Retrieving {} notifications for user: {}", category, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        List<Notification> notifications = notificationService.getNotificationsByCategory(userId, category);
        return ResponseEntity.ok(notifications);
    }

    // Get notifications by priority
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Notification>> getNotificationsByPriority(
            @PathVariable String priority,
            Authentication authentication) {
        
        logger.info("Retrieving {} priority notifications for user: {}", priority, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        List<Notification> notifications = notificationService.getNotificationsByPriority(userId, priority);
        return ResponseEntity.ok(notifications);
    }

    // Get recent notifications
    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @RequestParam(defaultValue = "24") int hours,
            Authentication authentication) {
        
        logger.info("Retrieving recent notifications (last {} hours) for user: {}", hours, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        List<Notification> notifications = notificationService.getRecentNotifications(userId, hours);
        return ResponseEntity.ok(notifications);
    }

    // Mark notification as read
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {
        
        logger.info("Marking notification {} as read for user: {}", notificationId, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    // Mark all notifications as read
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        logger.info("Marking all notifications as read for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // Mark category notifications as read
    @PutMapping("/category/{category}/read")
    public ResponseEntity<Map<String, String>> markCategoryAsRead(
            @PathVariable String category,
            Authentication authentication) {
        
        logger.info("Marking {} notifications as read for user: {}", category, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.markCategoryAsRead(userId, category);
        return ResponseEntity.ok(Map.of("message", category + " notifications marked as read"));
    }

    // Mark multiple notifications as read
    @PutMapping("/read")
    public ResponseEntity<Map<String, String>> markMultipleAsRead(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        
        List<Long> notificationIds = request.get("notificationIds");
        logger.info("Marking {} notifications as read for user: {}", 
                notificationIds.size(), authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.markAsReadByIds(notificationIds, userId);
        return ResponseEntity.ok(Map.of("message", "Notifications marked as read"));
    }

    // Delete notification
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {
        
        logger.info("Deleting notification {} for user: {}", notificationId, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    // Delete multiple notifications
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteMultipleNotifications(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        
        List<Long> notificationIds = request.get("notificationIds");
        logger.info("Deleting {} notifications for user: {}", 
                notificationIds.size(), authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.deleteMultipleNotifications(notificationIds, userId);
        return ResponseEntity.ok(Map.of("message", "Notifications deleted"));
    }

    // Delete old notifications
    @DeleteMapping("/old")
    public ResponseEntity<Map<String, String>> deleteOldNotifications(
            @RequestParam(defaultValue = "30") int daysOld,
            Authentication authentication) {
        
        logger.info("Deleting notifications older than {} days for user: {}", 
                daysOld, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.deleteOldNotifications(userId, daysOld);
        return ResponseEntity.ok(Map.of("message", "Old notifications deleted"));
    }

    // Get notification counts
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getNotificationCounts(Authentication authentication) {
        logger.info("Retrieving notification counts for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Map<String, Long> counts = notificationService.getNotificationCounts(userId);
        return ResponseEntity.ok(counts);
    }

    // Get unread count
    @GetMapping("/counts/unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // Get unread count by type
    @GetMapping("/counts/unread/type/{type}")
    public ResponseEntity<Map<String, Long>> getUnreadCountByType(
            @PathVariable NotificationType type,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        long count = notificationService.getUnreadCountByType(userId, type);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // Get unread count by category
    @GetMapping("/counts/unread/category/{category}")
    public ResponseEntity<Map<String, Long>> getUnreadCountByCategory(
            @PathVariable String category,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        long count = notificationService.getUnreadCountByCategory(userId, category);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // Update notification preferences
    @PutMapping("/preferences")
    public ResponseEntity<Map<String, String>> updateNotificationPreferences(
            @RequestBody Map<String, Boolean> preferences,
            Authentication authentication) {
        
        logger.info("Updating notification preferences for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.updateNotificationPreferences(userId, preferences);
        return ResponseEntity.ok(Map.of("message", "Notification preferences updated"));
    }

    // Get notification preferences
    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Boolean>> getNotificationPreferences(Authentication authentication) {
        logger.info("Retrieving notification preferences for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Map<String, Boolean> preferences = notificationService.getNotificationPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    // Search notifications
    @GetMapping("/search")
    public ResponseEntity<Page<Notification>> searchNotifications(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        logger.info("Searching notifications with query '{}' for user: {}", query, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Page<Notification> notifications = notificationService.searchNotifications(userId, query, pageable);
        return ResponseEntity.ok(notifications);
    }

    // Filter notifications
    @GetMapping("/filter")
    public ResponseEntity<Page<Notification>> filterNotifications(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        logger.info("Filtering notifications for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Page<Notification> notifications = notificationService.filterNotifications(
                userId, type, category, priority, isRead, startDate, endDate, pageable);
        return ResponseEntity.ok(notifications);
    }

    // Get grouped notifications
    @GetMapping("/group/{groupKey}")
    public ResponseEntity<List<Notification>> getGroupedNotifications(
            @PathVariable String groupKey,
            Authentication authentication) {
        
        logger.info("Retrieving grouped notifications '{}' for user: {}", groupKey, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        List<Notification> notifications = notificationService.getGroupedNotifications(userId, groupKey);
        return ResponseEntity.ok(notifications);
    }

    // Get notification statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getNotificationStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        
        logger.info("Retrieving notification statistics for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Map<String, Object> statistics;
        
        if (startDate != null && endDate != null) {
            statistics = notificationService.getNotificationStatistics(userId, startDate, endDate);
        } else {
            statistics = notificationService.getNotificationStatistics(userId);
        }
        
        return ResponseEntity.ok(statistics);
    }

    // Get notification trend data
    @GetMapping("/trends")
    public ResponseEntity<List<Map<String, Object>>> getNotificationTrendData(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        
        logger.info("Retrieving notification trend data ({} days) for user: {}", days, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        List<Map<String, Object>> trendData = notificationService.getNotificationTrendData(userId, days);
        return ResponseEntity.ok(trendData);
    }

    // Create notification from template
    @PostMapping("/template/{templateName}")
    public ResponseEntity<Notification> createFromTemplate(
            @PathVariable String templateName,
            @RequestBody Map<String, Object> variables,
            Authentication authentication) {
        
        logger.info("Creating notification from template '{}' for user: {}", templateName, authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        Notification notification = notificationService.createFromTemplate(templateName, userId, variables);
        return ResponseEntity.ok(notification);
    }

    // Send email notification
    @PostMapping("/{notificationId}/email")
    public ResponseEntity<Map<String, String>> sendEmailNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {
        
        logger.info("Sending email for notification {} requested by user: {}", 
                notificationId, authentication.getName());
        
        notificationService.sendEmailNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Email notification sent"));
    }

    // Send push notification
    @PostMapping("/{notificationId}/push")
    public ResponseEntity<Map<String, String>> sendPushNotification(
            @PathVariable Long notificationId,
            Authentication authentication) {
        
        logger.info("Sending push notification {} requested by user: {}", 
                notificationId, authentication.getName());
        
        notificationService.sendPushNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Push notification sent"));
    }

    // Send bulk email notifications
    @PostMapping("/bulk/email")
    public ResponseEntity<Map<String, String>> sendBulkEmailNotifications(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        
        List<Long> notificationIds = request.get("notificationIds");
        logger.info("Sending bulk email notifications for {} notifications requested by user: {}", 
                notificationIds.size(), authentication.getName());
        
        notificationService.sendBulkEmailNotifications(notificationIds);
        return ResponseEntity.ok(Map.of("message", "Bulk email notifications sent"));
    }

    // Send bulk push notifications
    @PostMapping("/bulk/push")
    public ResponseEntity<Map<String, String>> sendBulkPushNotifications(
            @RequestBody Map<String, List<Long>> request,
            Authentication authentication) {
        
        List<Long> notificationIds = request.get("notificationIds");
        logger.info("Sending bulk push notifications for {} notifications requested by user: {}", 
                notificationIds.size(), authentication.getName());
        
        notificationService.sendBulkPushNotifications(notificationIds);
        return ResponseEntity.ok(Map.of("message", "Bulk push notifications sent"));
    }

    // Check if notification type is enabled
    @GetMapping("/preferences/check/{type}")
    public ResponseEntity<Map<String, Boolean>> isNotificationEnabled(
            @PathVariable NotificationType type,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        boolean enabled = notificationService.isNotificationEnabled(userId, type);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    // Group similar notifications
    @PostMapping("/group")
    public ResponseEntity<Map<String, String>> groupSimilarNotifications(Authentication authentication) {
        logger.info("Grouping similar notifications for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        notificationService.groupSimilarNotifications(userId);
        return ResponseEntity.ok(Map.of("message", "Similar notifications grouped"));
    }

    // Test notification creation (for development/testing)
    @PostMapping("/test")
    public ResponseEntity<Notification> createTestNotification(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        logger.info("Creating test notification for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        NotificationType type = NotificationType.valueOf((String) request.get("type"));
        String title = (String) request.get("title");
        String message = (String) request.get("message");
        
        Notification notification = notificationService.createNotification(userId, type, title, message);
        return ResponseEntity.ok(notification);
    }

    // Administrative endpoints (should be secured for admin users only)
    
    // Process notification queue (admin)
    @PostMapping("/admin/process-queue")
    public ResponseEntity<Map<String, String>> processNotificationQueue() {
        logger.info("Processing notification queue");
        
        notificationService.processNotificationQueue();
        return ResponseEntity.ok(Map.of("message", "Notification queue processed"));
    }

    // Retry failed notifications (admin)
    @PostMapping("/admin/retry-failed")
    public ResponseEntity<Map<String, String>> retryFailedNotifications() {
        logger.info("Retrying failed notifications");
        
        notificationService.retryFailedNotifications();
        return ResponseEntity.ok(Map.of("message", "Failed notifications retried"));
    }

    // Cleanup expired notifications (admin)
    @PostMapping("/admin/cleanup-expired")
    public ResponseEntity<Map<String, String>> cleanupExpiredNotifications() {
        logger.info("Cleaning up expired notifications");
        
        notificationService.cleanupExpiredNotifications();
        return ResponseEntity.ok(Map.of("message", "Expired notifications cleaned up"));
    }

    // Cleanup old read notifications (admin)
    @PostMapping("/admin/cleanup-old")
    public ResponseEntity<Map<String, String>> cleanupOldReadNotifications(
            @RequestParam(defaultValue = "90") int daysOld) {
        
        logger.info("Cleaning up read notifications older than {} days", daysOld);
        
        notificationService.cleanupOldReadNotifications(daysOld);
        return ResponseEntity.ok(Map.of("message", "Old read notifications cleaned up"));
    }

    // Register notification template (admin)
    @PostMapping("/admin/templates")
    public ResponseEntity<Map<String, String>> registerNotificationTemplate(
            @RequestBody Map<String, String> request) {
        
        String templateName = request.get("templateName");
        String titleTemplate = request.get("titleTemplate");
        String messageTemplate = request.get("messageTemplate");
        
        logger.info("Registering notification template: {}", templateName);
        
        notificationService.registerNotificationTemplate(templateName, titleTemplate, messageTemplate);
        return ResponseEntity.ok(Map.of("message", "Notification template registered"));
    }

    // Get notifications for retry (admin)
    @GetMapping("/admin/retry-candidates")
    public ResponseEntity<List<Notification>> getNotificationsForRetry() {
        logger.info("Retrieving notifications for retry");
        
        List<Notification> notifications = notificationService.findNotificationsForRetry();
        return ResponseEntity.ok(notifications);
    }

    // WebSocket endpoint for real-time notifications
    @GetMapping("/realtime/connect")
    public ResponseEntity<Map<String, String>> connectRealTime(Authentication authentication) {
        logger.info("Establishing real-time notification connection for user: {}", authentication.getName());
        
        // Implementation would depend on WebSocket setup
        return ResponseEntity.ok(Map.of("message", "Real-time connection established"));
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "notification-service"));
    }

    // Export notifications (for user data export)
    @GetMapping("/export")
    public ResponseEntity<List<Notification>> exportNotifications(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        
        logger.info("Exporting notifications for user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        List<Notification> notifications;
        
        if (startDate != null && endDate != null) {
            // Filter by date range if provided
            notifications = notificationService.getUserNotifications(userId).stream()
                    .filter(n -> !n.getCreatedAt().isBefore(startDate) && !n.getCreatedAt().isAfter(endDate))
                    .toList();
        } else {
            notifications = notificationService.getUserNotifications(userId);
        }
        
        return ResponseEntity.ok(notifications);
    }

    // Helper method to extract user ID from authentication
    private Long getUserIdFromAuth(Authentication authentication) {
        // This implementation depends on your security setup
        // You might need to look up the user by username
        return 1L; // Placeholder - implement based on your User entity and security configuration
    }

    // Exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("Error in NotificationController", e);
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}