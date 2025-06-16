package com.trackify.service.impl;

import com.trackify.entity.Notification;
import com.trackify.entity.User;
import com.trackify.entity.Expense;
import com.trackify.entity.Budget;
import com.trackify.entity.ApprovalWorkflow;
import com.trackify.enums.NotificationType;
import com.trackify.exception.ResourceNotFoundException;
import com.trackify.exception.ForbiddenException;
import com.trackify.repository.*;
import com.trackify.service.NotificationService;
import com.trackify.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ApprovalWorkflowRepository approvalWorkflowRepository;

    @Autowired
    private EmailService emailService;

    // In-memory template storage (in production, use database)
    private final Map<String, NotificationTemplate> templates = new ConcurrentHashMap<>();
    
    // In-memory user preferences (in production, use database)
    private final Map<Long, Map<String, Boolean>> userPreferences = new ConcurrentHashMap<>();

    @Override
    public Notification createNotification(Long userId, NotificationType type, String title, String message) {
        try {
            Notification notification = new Notification(userId, type, title, message);
            
            // Set category based on type
            notification.setCategory(determineCategoryFromType(type));
            
            // Set priority based on type
            notification.setPriority(determinePriorityFromType(type));
            
            // Set expiration if needed
            if (shouldExpire(type)) {
                notification.setExpiration(getExpirationHours(type));
            }

            Notification saved = notificationRepository.save(notification);
            logger.info("Created notification for user {} with type {}", userId, type);
            
            // Queue for email/push if enabled
            queueForDelivery(saved);
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Error creating notification for user {}", userId, e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    @Override
    public Notification createNotification(Long userId, NotificationType type, String title, String message, 
                                          String entityType, Long entityId) {
        Notification notification = createNotification(userId, type, title, message);
        notification.setRelatedEntityType(entityType);
        notification.setRelatedEntityId(entityId);
        
        // Generate group key for related notifications
        notification.setGroupKey(generateGroupKey(type, entityType, entityId));
        
        return notificationRepository.save(notification);
    }
    
    @Override
    public Notification createNotification(Long userId, NotificationType type, String title, String message, String priority) {
        Notification notification = createNotification(userId, type, title, message);
        notification.setPriority(priority);
        return notificationRepository.save(notification);
    }
    @Override
    public Notification createNotification(Long userId, NotificationType type, String title, String message,
                                          String entityType, Long entityId, String priority) {
        Notification notification = createNotification(userId, type, title, message, entityType, entityId);
        notification.setPriority(priority);
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> createBulkNotifications(List<Long> userIds, NotificationType type, 
                                                      String title, String message) {
        List<Notification> notifications = new ArrayList<>();
        
        for (Long userId : userIds) {
            if (canReceiveNotification(userId, type)) {
                Notification notification = new Notification(userId, type, title, message);
                notification.setCategory(determineCategoryFromType(type));
                notification.setPriority(determinePriorityFromType(type));
                notifications.add(notification);
            }
        }
        
        List<Notification> saved = notificationRepository.saveAll(notifications);
        logger.info("Created {} bulk notifications of type {}", saved.size(), type);
        
        // Queue for delivery
        saved.forEach(this::queueForDelivery);
        
        return saved;
    }

    @Override
    public List<Notification> createBulkNotifications(List<Long> userIds, NotificationType type, 
                                                      String title, String message, String entityType, Long entityId) {
        List<Notification> notifications = createBulkNotifications(userIds, type, title, message);
        
        notifications.forEach(notification -> {
            notification.setRelatedEntityType(entityType);
            notification.setRelatedEntityId(entityId);
            notification.setGroupKey(generateGroupKey(type, entityType, entityId));
        });
        
        return notificationRepository.saveAll(notifications);
    }

    // Expense-related notifications
    @Override
    public void notifyExpenseSubmitted(Long expenseId, Long submitterId, Long approverId) {
        try {
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
            
            User submitter = userRepository.findById(submitterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Submitter not found"));
            
            // Notify approver
            String title = "New Expense Awaiting Approval";
            String message = String.format("New expense '%s' (₹%.2f) submitted by %s requires your approval", 
                    expense.getTitle(), expense.getAmount(), submitter.getFullName());
            
            Notification approverNotification = createNotification(approverId, NotificationType.EXPENSE_PENDING_APPROVAL, 
                    title, message, "EXPENSE", expenseId, "HIGH");
            approverNotification.setActionButton("/expenses/" + expenseId + "/approve", "Review");
            notificationRepository.save(approverNotification);
            
            // Notify submitter
            String submitterTitle = "Expense Submitted Successfully";
            String submitterMessage = String.format("Your expense '%s' has been submitted and is awaiting approval", 
                    expense.getTitle());
            
            createNotification(submitterId, NotificationType.EXPENSE_SUBMITTED, 
                    submitterTitle, submitterMessage, "EXPENSE", expenseId);
            
            logger.info("Sent expense submission notifications for expense {}", expenseId);
            
        } catch (Exception e) {
            logger.error("Error sending expense submission notifications for expense {}", expenseId, e);
        }
    }

    @Override
    public void notifyExpenseApproved(Long expenseId, Long submitterId, Long approverId) {
        try {
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
            
            User approver = userRepository.findById(approverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));
            
            String title = "Expense Approved";
            String message = String.format("Your expense '%s' (₹%.2f) has been approved by %s", 
                    expense.getTitle(), expense.getAmount(), approver.getFullName());
            
            Notification notification = createNotification(submitterId, NotificationType.EXPENSE_APPROVED, 
                    title, message, "EXPENSE", expenseId);
            notification.setActionButton("/expenses/" + expenseId, "View Details");
            notificationRepository.save(notification);
            
            logger.info("Sent expense approval notification for expense {}", expenseId);
            
        } catch (Exception e) {
            logger.error("Error sending expense approval notification for expense {}", expenseId, e);
        }
    }

    @Override
    public void notifyExpenseRejected(Long expenseId, Long submitterId, Long approverId, String reason) {
        try {
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
            
            User approver = userRepository.findById(approverId)
                    .orElseThrow(() -> new ResourceNotFoundException("Approver not found"));
            
            String title = "Expense Rejected";
            String message = String.format("Your expense '%s' (₹%.2f) has been rejected by %s. Reason: %s", 
                    expense.getTitle(), expense.getAmount(), approver.getFullName(), reason);
            
            Notification notification = createNotification(submitterId, NotificationType.EXPENSE_REJECTED, 
                    title, message, "EXPENSE", expenseId, "HIGH");
            notification.setActionButton("/expenses/" + expenseId + "/edit", "Edit & Resubmit");
            notificationRepository.save(notification);
            
            logger.info("Sent expense rejection notification for expense {}", expenseId);
            
        } catch (Exception e) {
            logger.error("Error sending expense rejection notification for expense {}", expenseId, e);
        }
    }

    @Override
    public void notifyExpenseEscalated(Long expenseId, Long submitterId, Long newApproverId) {
        try {
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
            
            // Notify new approver
            String title = "Escalated Expense Requires Approval";
            String message = String.format("Escalated expense '%s' (₹%.2f) requires your urgent approval", 
                    expense.getTitle(), expense.getAmount());
            
            Notification approverNotification = createNotification(newApproverId, NotificationType.EXPENSE_ESCALATED, 
                    title, message, "EXPENSE", expenseId, "URGENT");
            approverNotification.setActionButton("/expenses/" + expenseId + "/approve", "Review Urgently");
            notificationRepository.save(approverNotification);
            
            // Notify submitter
            String submitterTitle = "Expense Escalated";
            String submitterMessage = String.format("Your expense '%s' has been escalated for approval", 
                    expense.getTitle());
            
            createNotification(submitterId, NotificationType.EXPENSE_ESCALATED, 
                    submitterTitle, submitterMessage, "EXPENSE", expenseId);
            
            logger.info("Sent expense escalation notifications for expense {}", expenseId);
            
        } catch (Exception e) {
            logger.error("Error sending expense escalation notifications for expense {}", expenseId, e);
        }
    }

    @Override
    public void notifyExpenseOverdue(Long expenseId, Long submitterId, Long approverId) {
        try {
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
            
            // Notify approver
            String title = "Overdue Expense Approval";
            String message = String.format("Expense '%s' (₹%.2f) approval is overdue. Please review immediately", 
                    expense.getTitle(), expense.getAmount());
            
            Notification approverNotification = createNotification(approverId, NotificationType.EXPENSE_OVERDUE, 
                    title, message, "EXPENSE", expenseId, "URGENT");
            approverNotification.setActionButton("/expenses/" + expenseId + "/approve", "Review Now");
            notificationRepository.save(approverNotification);
            
            // Notify submitter
            String submitterTitle = "Expense Approval Overdue";
            String submitterMessage = String.format("Approval for your expense '%s' is overdue", 
                    expense.getTitle());
            
            createNotification(submitterId, NotificationType.EXPENSE_OVERDUE, 
                    submitterTitle, submitterMessage, "EXPENSE", expenseId);
            
            logger.info("Sent expense overdue notifications for expense {}", expenseId);
            
        } catch (Exception e) {
            logger.error("Error sending expense overdue notifications for expense {}", expenseId, e);
        }
    }

    // Budget-related notifications
    @Override
    public void notifyBudgetExceeded(Long budgetId, Long userId, String budgetName, Double percentage) {
        String title = "Budget Exceeded";
        String message = String.format("Budget '%s' has been exceeded by %.1f%%. Please review spending", 
                budgetName, percentage);
        
        Notification notification = createNotification(userId, NotificationType.BUDGET_EXCEEDED, 
                title, message, "BUDGET", budgetId, "URGENT");
        notification.setActionButton("/budgets/" + budgetId, "View Budget");
        notificationRepository.save(notification);
        
        logger.info("Sent budget exceeded notification for budget {} to user {}", budgetId, userId);
    }

    @Override
    public void notifyBudgetWarning(Long budgetId, Long userId, String budgetName, Double percentage) {
        String title = "Budget Warning";
        String message = String.format("Budget '%s' is %.1f%% utilized. Consider monitoring expenses", 
                budgetName, percentage);
        
        Notification notification = createNotification(userId, NotificationType.BUDGET_WARNING, 
                title, message, "BUDGET", budgetId, "HIGH");
        notification.setActionButton("/budgets/" + budgetId, "View Budget");
        notificationRepository.save(notification);
        
        logger.info("Sent budget warning notification for budget {} to user {}", budgetId, userId);
    }

    @Override
    public void notifyBudgetExpired(Long budgetId, List<Long> userIds, String budgetName) {
        String title = "Budget Expired";
        String message = String.format("Budget '%s' has expired. Please create a new budget for continued tracking", 
                budgetName);
        
        createBulkNotifications(userIds, NotificationType.BUDGET_EXPIRED, title, message, "BUDGET", budgetId);
        
        logger.info("Sent budget expired notifications for budget {} to {} users", budgetId, userIds.size());
    }

    // Comment-related notifications
    @Override
    public void notifyCommentAdded(Long commentId, Long expenseId, Long authorId, List<Long> recipientIds) {
        try {
            User author = userRepository.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
            
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
            
            String title = "New Comment Added";
            String message = String.format("%s added a comment to expense '%s'", 
                    author.getFullName(), expense.getTitle());
            
            createBulkNotifications(recipientIds, NotificationType.COMMENT_ADDED, title, message, "COMMENT", commentId);
            
            logger.info("Sent comment added notifications for comment {} to {} recipients", commentId, recipientIds.size());
            
        } catch (Exception e) {
            logger.error("Error sending comment added notifications for comment {}", commentId, e);
        }
    }

    @Override
    public void notifyCommentReply(Long commentId, Long parentCommentId, Long authorId, Long recipientId) {
        try {
            User author = userRepository.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
            
            String title = "Comment Reply";
            String message = String.format("%s replied to your comment", author.getFullName());
            
            Notification notification = createNotification(recipientId, NotificationType.COMMENT_REPLY, 
                    title, message, "COMMENT", commentId);
            notification.setActionButton("/comments/" + parentCommentId, "View Thread");
            notificationRepository.save(notification);
            
            logger.info("Sent comment reply notification for comment {} to user {}", commentId, recipientId);
            
        } catch (Exception e) {
            logger.error("Error sending comment reply notification for comment {}", commentId, e);
        }
    }

    @Override
    public void notifyCommentMention(Long commentId, Long authorId, List<Long> mentionedUserIds) {
        try {
            User author = userRepository.findById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
            
            String title = "You were mentioned";
            String message = String.format("%s mentioned you in a comment", author.getFullName());
            
            createBulkNotifications(mentionedUserIds, NotificationType.COMMENT_MENTION, title, message, "COMMENT", commentId);
            
            logger.info("Sent comment mention notifications for comment {} to {} users", commentId, mentionedUserIds.size());
            
        } catch (Exception e) {
            logger.error("Error sending comment mention notifications for comment {}", commentId, e);
        }
    }

    // Approval-related notifications
    @Override
    public void notifyApprovalRequest(Long workflowId, Long expenseId, Long submitterId, Long approverId) {
        String title = "Approval Request";
        String message = "You have a new expense approval request";
        
        Notification notification = createNotification(approverId, NotificationType.APPROVAL_REQUEST, 
                title, message, "APPROVAL", workflowId, "HIGH");
        notification.setActionButton("/approvals/" + workflowId, "Review");
        notificationRepository.save(notification);
        
        logger.info("Sent approval request notification for workflow {} to approver {}", workflowId, approverId);
    }

    @Override
    public void notifyApprovalReminder(Long workflowId, Long approverId) {
        String title = "Approval Reminder";
        String message = "You have pending approval requests that require attention";
        
        Notification notification = createNotification(approverId, NotificationType.APPROVAL_REMINDER, 
                title, message, "APPROVAL", workflowId, "HIGH");
        notification.setActionButton("/approvals/pending", "View Pending");
        notificationRepository.save(notification);
        
        logger.info("Sent approval reminder notification for workflow {} to approver {}", workflowId, approverId);
    }

    @Override
    public void notifyApprovalEscalation(Long workflowId, Long fromApproverId, Long toApproverId) {
        String title = "Escalated Approval";
        String message = "An approval request has been escalated to you";
        
        Notification notification = createNotification(toApproverId, NotificationType.APPROVAL_ESCALATION, 
                title, message, "APPROVAL", workflowId, "URGENT");
        notification.setActionButton("/approvals/" + workflowId, "Review Urgently");
        notificationRepository.save(notification);
        
        logger.info("Sent approval escalation notification for workflow {} from {} to {}", 
                workflowId, fromApproverId, toApproverId);
    }

    // System notifications
    @Override
    public void notifySystemUpdate(List<Long> userIds, String title, String message) {
        createBulkNotifications(userIds, NotificationType.SYSTEM_UPDATE, title, message);
        logger.info("Sent system update notifications to {} users", userIds.size());
    }

    @Override
    public void notifyReportGenerated(Long userId, String reportName, String downloadUrl) {
        String title = "Report Ready";
        String message = String.format("Your report '%s' has been generated and is ready for download", reportName);
        
        Notification notification = createNotification(userId, NotificationType.REPORT_READY, title, message);
        notification.setActionButton(downloadUrl, "Download");
        notificationRepository.save(notification);
        
        logger.info("Sent report ready notification to user {} for report {}", userId, reportName);
    }

    @Override
    public void notifyPolicyViolation(Long userId, Long expenseId, String violation) {
        String title = "Policy Violation Detected";
        String message = String.format("Policy violation detected: %s", violation);
        
        Notification notification = createNotification(userId, NotificationType.POLICY_VIOLATION, 
                title, message, "EXPENSE", expenseId, "URGENT");
        notification.setActionButton("/expenses/" + expenseId, "Review");
        notificationRepository.save(notification);
        
        logger.info("Sent policy violation notification to user {} for expense {}", userId, expenseId);
    }

    @Override
    public void notifyDuplicateExpense(Long userId, Long expenseId, Long duplicateExpenseId) {
        String title = "Duplicate Expense Detected";
        String message = "A potential duplicate expense has been detected";
        
        Notification notification = createNotification(userId, NotificationType.DUPLICATE_EXPENSE, 
                title, message, "EXPENSE", expenseId, "HIGH");
        notification.setActionButton("/expenses/" + expenseId + "/compare/" + duplicateExpenseId, "Compare");
        notificationRepository.save(notification);
        
        logger.info("Sent duplicate expense notification to user {} for expenses {} and {}", 
                userId, expenseId, duplicateExpenseId);
    }

    // Security notifications
    @Override
    public void notifyAccountLocked(Long userId) {
        String title = "Account Locked";
        String message = "Your account has been locked due to security reasons. Please contact support";
        
        Notification notification = createNotification(userId, NotificationType.ACCOUNT_LOCKED, title, message, "URGENT");
        notification.setActionButton("/support/contact", "Contact Support");
        notificationRepository.save(notification);
        
        logger.info("Sent account locked notification to user {}", userId);
    }

    @Override
    public void notifyPasswordReset(Long userId) {
        String title = "Password Reset";
        String message = "Your password has been reset successfully";
        
        createNotification(userId, NotificationType.PASSWORD_RESET, title, message);
        
        logger.info("Sent password reset notification to user {}", userId);
    }

    @Override
    public void notifyLoginAlert(Long userId, String location, String device) {
        String title = "New Login Detected";
        String message = String.format("New login from %s on %s", location, device);
        
        createNotification(userId, NotificationType.LOGIN_ALERT, title, message, "HIGH");
        
        logger.info("Sent login alert notification to user {} for location {} device {}", userId, location, device);
    }

    // AI-related notifications
    @Override
    public void notifyAiSuggestion(Long userId, String suggestion, String entityType, Long entityId) {
        String title = "AI Suggestion";
        String message = String.format("AI suggestion: %s", suggestion);
        
        createNotification(userId, NotificationType.AI_SUGGESTION, title, message, entityType, entityId);
        
        logger.info("Sent AI suggestion notification to user {} for {} {}", userId, entityType, entityId);
    }

    @Override
    public void notifyAnomalyDetected(Long userId, String anomaly, String entityType, Long entityId) {
        String title = "Anomaly Detected";
        String message = String.format("Anomaly detected: %s", anomaly);
        
        Notification notification = createNotification(userId, NotificationType.ANOMALY_DETECTED, 
                title, message, entityType, entityId, "URGENT");
        notification.setActionButton("/" + entityType.toLowerCase() + "s/" + entityId, "Investigate");
        notificationRepository.save(notification);
        
        logger.info("Sent anomaly detection notification to user {} for {} {}", userId, entityType, entityId);
    }

    // Retrieve notifications
    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByType(Long userId, NotificationType type) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByCategory(Long userId, String category) {
        return notificationRepository.findByUserIdAndCategoryOrderByCreatedAtDesc(userId, category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByPriority(Long userId, String priority) {
        return notificationRepository.findByUserIdAndPriorityOrderByCreatedAtDesc(userId, priority);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return notificationRepository.findRecentNotificationsByUser(userId, since);
    }

    // Notification management
    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied to notification");
        }
        
        notification.markAsRead();
        notificationRepository.save(notification);
        
        logger.info("Marked notification {} as read for user {}", notificationId, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadByUser(userId, LocalDateTime.now());
        logger.info("Marked {} notifications as read for user {}", count, userId);
    }

    @Override
    public void markCategoryAsRead(Long userId, String category) {
        int count = notificationRepository.markCategoryAsReadByUser(userId, category, LocalDateTime.now());
        logger.info("Marked {} {} notifications as read for user {}", count, category, userId);
    }

    @Override
    public void markAsReadByIds(List<Long> notificationIds, Long userId) {
        // Verify ownership
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        boolean allOwnedByUser = notifications.stream().allMatch(n -> n.getUserId().equals(userId));
        
        if (!allOwnedByUser) {
            throw new ForbiddenException("Access denied to one or more notifications");
        }
        
        int count = notificationRepository.markAsReadByIds(notificationIds, LocalDateTime.now());
        logger.info("Marked {} notifications as read for user {}", count, userId);
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied to notification");
        }
        
        notificationRepository.delete(notification);
        logger.info("Deleted notification {} for user {}", notificationId, userId);
    }

    @Override
    public void deleteOldNotifications(Long userId, int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<Notification> oldNotifications = notificationRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, LocalDateTime.MIN, cutoffDate);
        
        notificationRepository.deleteAll(oldNotifications);
        logger.info("Deleted {} old notifications for user {}", oldNotifications.size(), userId);
    }

    // Notification counts
    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCountByType(Long userId, NotificationType type) {
        return notificationRepository.countByUserIdAndTypeAndIsReadFalse(userId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCountByCategory(Long userId, String category) {
        return notificationRepository.countByUserIdAndCategoryAndIsReadFalse(userId, category);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getNotificationCounts(Long userId) {
        Map<String, Long> counts = new HashMap<>();
        counts.put("total", notificationRepository.countByUserIdAndIsReadFalse(userId));
        counts.put("high_priority", notificationRepository.countByUserIdAndPriorityAndIsReadFalse(userId, "HIGH"));
        counts.put("urgent", notificationRepository.countByUserIdAndPriorityAndIsReadFalse(userId, "URGENT"));
        
        // Category counts
        Arrays.asList("EXPENSE", "BUDGET", "APPROVAL", "COMMENT", "SYSTEM", "SECURITY").forEach(category -> 
            counts.put(category.toLowerCase(), getUnreadCountByCategory(userId, category)));
        
        return counts;
    }

    // Notification settings and preferences
    @Override
    public void updateNotificationPreferences(Long userId, Map<String, Boolean> preferences) {
        userPreferences.put(userId, new HashMap<>(preferences));
        logger.info("Updated notification preferences for user {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Boolean> getNotificationPreferences(Long userId) {
        return userPreferences.getOrDefault(userId, getDefaultPreferences());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationEnabled(Long userId, NotificationType type) {
        Map<String, Boolean> preferences = getNotificationPreferences(userId);
        return preferences.getOrDefault(type.name(), true);
    }

    // Email and push notifications
    @Override
    public void sendEmailNotification(Long notificationId) {
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
            
            if (notification.needsEmailNotification() && isNotificationEnabled(notification.getUserId(), notification.getType())) {
                User user = userRepository.findById(notification.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
                // Send email using EmailService
                emailService.sendHtmlEmail(user.getEmail(), notification.getTitle(), notification.getMessage());
                markEmailSent(notificationId);
                logger.info("Sent email notification {}", notificationId);
            }
        } catch (Exception e) {
            logger.error("Error sending email notification {}", notificationId, e);
            updateRetryCount(notificationId);
        }
    }

    @Override
    public void sendPushNotification(Long notificationId) {
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
            
            if (notification.needsPushNotification() && isNotificationEnabled(notification.getUserId(), notification.getType())) {
                // Implement push notification logic here
                markPushSent(notificationId);
                logger.info("Sent push notification {}", notificationId);
            }
        } catch (Exception e) {
            logger.error("Error sending push notification {}", notificationId, e);
            updateRetryCount(notificationId);
        }
    }

    @Override
    public void sendBulkEmailNotifications(List<Long> notificationIds) {
        notificationIds.forEach(this::sendEmailNotification);
    }

    @Override
    public void sendBulkPushNotifications(List<Long> notificationIds) {
        notificationIds.forEach(this::sendPushNotification);
    }

    // Notification delivery
    @Override
    public void processNotificationQueue() {
        LocalDateTime now = LocalDateTime.now();
        
        // Process email notifications
        List<Notification> emailQueue = notificationRepository.findNotificationsNeedingEmail(now);
        emailQueue.forEach(notification -> sendEmailNotification(notification.getId()));
        
        // Process push notifications
        List<Notification> pushQueue = notificationRepository.findNotificationsNeedingPush(now);
        pushQueue.forEach(notification -> sendPushNotification(notification.getId()));
        
        logger.info("Processed {} email and {} push notifications", emailQueue.size(), pushQueue.size());
    }

    @Override
    public void retryFailedNotifications() {
        LocalDateTime retryBefore = LocalDateTime.now().minusHours(1);
        LocalDateTime now = LocalDateTime.now();
        
        List<Notification> retryList = notificationRepository.findNotificationsForRetry(retryBefore, now);
        
        for (Notification notification : retryList) {
            if (notification.canRetry()) {
                if (!notification.getIsEmailSent()) {
                    sendEmailNotification(notification.getId());
                }
                if (!notification.getIsPushSent()) {
                    sendPushNotification(notification.getId());
                }
            }
        }
        
        logger.info("Retried {} failed notifications", retryList.size());
    }

    @Override
    public void markEmailSent(Long notificationId) {
        notificationRepository.markEmailSent(notificationId, LocalDateTime.now());
    }

    @Override
    public void markPushSent(Long notificationId) {
        notificationRepository.markPushSent(notificationId, LocalDateTime.now());
    }

    // Notification grouping
    @Override
    public void groupSimilarNotifications(Long userId) {
        // Implementation for grouping similar notifications
        // This could involve analyzing notification types, entities, and timeframes
        logger.info("Grouped similar notifications for user {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getGroupedNotifications(Long userId, String groupKey) {
        return notificationRepository.findByUserIdAndGroupKeyOrderByCreatedAtDesc(userId, groupKey);
    }

    // Statistics and analytics
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics(Long userId) {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        return getNotificationStatistics(userId, monthAgo, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        List<Object[]> typeStats = notificationRepository.getNotificationTypeStatsByUser(userId, startDate);
        List<Object[]> categoryStats = notificationRepository.getNotificationCategoryStatsByUser(userId, startDate);
        List<Object[]> dailyStats = notificationRepository.getDailyNotificationStatsByUser(userId, startDate);
        
        stats.put("typeBreakdown", typeStats);
        stats.put("categoryBreakdown", categoryStats);
        stats.put("dailyTrend", dailyStats);
        stats.put("totalCount", getUnreadCount(userId));
        
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getNotificationTrendData(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> dailyStats = notificationRepository.getDailyNotificationStatsByUser(userId, startDate);
        
        return dailyStats.stream()
                .map(row -> Map.of("date", row[0], "count", row[1]))
                .collect(Collectors.toList());
    }

    // Administrative functions
    @Override
    public void cleanupExpiredNotifications() {
        int count = notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
        logger.info("Cleaned up {} expired notifications", count);
    }

    @Override
    public void cleanupOldReadNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int count = notificationRepository.deleteOldReadNotifications(cutoffDate);
        logger.info("Cleaned up {} old read notifications", count);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findNotificationsForRetry() {
        LocalDateTime retryBefore = LocalDateTime.now().minusHours(1);
        LocalDateTime now = LocalDateTime.now();
        return notificationRepository.findNotificationsForRetry(retryBefore, now);
    }

    @Override
    public void updateRetryCount(Long notificationId) {
        notificationRepository.incrementRetryCount(notificationId, LocalDateTime.now());
    }

    // Search and filtering
    @Override
    @Transactional(readOnly = true)
    public Page<Notification> searchNotifications(Long userId, String query, Pageable pageable) {
        // Simple implementation - in production, use full-text search
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> filterNotifications(Long userId, NotificationType type, String category,
                                                  String priority, Boolean isRead, LocalDateTime startDate,
                                                  LocalDateTime endDate, Pageable pageable) {
        return notificationRepository.findByCriteria(userId, type, category, priority, isRead, 
                startDate, endDate, pageable);
    }

    // Batch operations
    @Override
    public void deleteMultipleNotifications(List<Long> notificationIds, Long userId) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        boolean allOwnedByUser = notifications.stream().allMatch(n -> n.getUserId().equals(userId));
        
        if (!allOwnedByUser) {
            throw new ForbiddenException("Access denied to one or more notifications");
        }
        
        notificationRepository.deleteAll(notifications);
        logger.info("Deleted {} notifications for user {}", notificationIds.size(), userId);
    }

    @Override
    public void markMultipleAsRead(List<Long> notificationIds, Long userId) {
        markAsReadByIds(notificationIds, userId);
    }

    // Real-time notifications
    @Override
    public void sendRealTimeNotification(Long userId, Notification notification) {
        // Implementation for real-time notification (WebSocket, Server-Sent Events, etc.)
        logger.info("Sent real-time notification to user {}", userId);
    }

    @Override
    public void broadcastNotification(List<Long> userIds, Notification notification) {
        userIds.forEach(userId -> sendRealTimeNotification(userId, notification));
    }

    // Template-based notifications
    @Override
    public Notification createFromTemplate(String templateName, Long userId, Map<String, Object> variables) {
        NotificationTemplate template = templates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        String title = processTemplate(template.titleTemplate, variables);
        String message = processTemplate(template.messageTemplate, variables);
        
        return createNotification(userId, template.type, title, message);
    }

    @Override
    public void registerNotificationTemplate(String templateName, String titleTemplate, String messageTemplate) {
        templates.put(templateName, new NotificationTemplate(titleTemplate, messageTemplate, NotificationType.SYSTEM_UPDATE));
        logger.info("Registered notification template: {}", templateName);
    }

    // Notification validation
    @Override
    public boolean validateNotification(Notification notification) {
        return notification.getUserId() != null &&
               notification.getType() != null &&
               notification.getTitle() != null && !notification.getTitle().trim().isEmpty() &&
               notification.getMessage() != null && !notification.getMessage().trim().isEmpty();
    }

    @Override
    public boolean canReceiveNotification(Long userId, NotificationType type) {
        return isNotificationEnabled(userId, type);
    }

    // Helper methods
    @Override
    public String generateGroupKey(NotificationType type, String entityType, Long entityId) {
        return String.format("%s_%s_%s", type.name(), entityType, entityId);
    }

    @Override
    public void scheduleNotification(Notification notification, LocalDateTime scheduledTime) {
        // Implementation for scheduling notifications
        logger.info("Scheduled notification for {}", scheduledTime);
    }

    @Override
    public void cancelScheduledNotification(Long notificationId) {
        // Implementation for canceling scheduled notifications
        logger.info("Cancelled scheduled notification {}", notificationId);
    }

    // Private helper methods
    private String determineCategoryFromType(NotificationType type) {
        if (type.isExpenseRelated()) return "EXPENSE";
        if (type.isBudgetRelated()) return "BUDGET";
        if (type.isApprovalRelated()) return "APPROVAL";
        if (type.isCommentRelated()) return "COMMENT";
        if (type.isSecurityRelated()) return "SECURITY";
        if (type.isSystemRelated()) return "SYSTEM";
        if (type.isAiRelated()) return "AI";
        return "GENERAL";
    }

    private String determinePriorityFromType(NotificationType type) {
        if (type.isUrgent()) return "URGENT";
        if (type.requiresAction()) return "HIGH";
        return "MEDIUM";
    }

    private boolean shouldExpire(NotificationType type) {
        return type == NotificationType.APPROVAL_REQUEST ||
               type == NotificationType.EXPENSE_PENDING_APPROVAL ||
               type == NotificationType.REPORT_READY;
    }

    private int getExpirationHours(NotificationType type) {
        switch (type) {
            case APPROVAL_REQUEST:
            case EXPENSE_PENDING_APPROVAL:
                return 168; // 7 days
            case REPORT_READY:
                return 72; // 3 days
            default:
                return 24; // 1 day
        }
    }

    private void queueForDelivery(Notification notification) {
        if (isNotificationEnabled(notification.getUserId(), notification.getType())) {
            // Queue for email/push notification processing
            logger.debug("Queued notification {} for delivery", notification.getId());
        }
    }

    private Map<String, Boolean> getDefaultPreferences() {
        Map<String, Boolean> defaults = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            defaults.put(type.name(), true);
        }
        return defaults;
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    // Inner class for notification templates
    private static class NotificationTemplate {
        final String titleTemplate;
        final String messageTemplate;
        final NotificationType type;

        NotificationTemplate(String titleTemplate, String messageTemplate, NotificationType type) {
            this.titleTemplate = titleTemplate;
            this.messageTemplate = messageTemplate;
            this.type = type;
        }
    }
}