package com.trackify.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.trackify.enums.NotificationType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_email_sent", nullable = false)
    private Boolean isEmailSent = false;

    @Column(name = "is_push_sent", nullable = false)
    private Boolean isPushSent = false;

    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM";

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "action_text", length = 100)
    private String actionText;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "push_sent_at")
    private LocalDateTime pushSentAt;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "is_system_generated", nullable = false)
    private Boolean isSystemGenerated = true;

    @Column(name = "group_key", length = 100)
    private String groupKey;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    @JsonIgnore
    private User sender;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor (required by Hibernate)
    public Notification() {
        this.isRead = false;
        this.isEmailSent = false;
        this.isPushSent = false;
        this.priority = "MEDIUM";
        this.isSystemGenerated = true;
        this.retryCount = 0;
    }

    // Basic constructor
    public Notification(Long userId, NotificationType type, String title, String message) {
        this();
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.category = determineCategoryFromType(type);
    }

    // Constructor with related entity
    public Notification(Long userId, NotificationType type, String title, String message, 
                       String relatedEntityType, Long relatedEntityId) {
        this(userId, type, title, message);
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
    }

    // Constructor with priority
    public Notification(Long userId, NotificationType type, String title, String message,
                       String relatedEntityType, Long relatedEntityId, String priority) {
        this(userId, type, title, message, relatedEntityType, relatedEntityId);
        this.priority = priority;
    }

    // Full constructor
    public Notification(Long id, Long userId, NotificationType type, String title, String message,
                       String relatedEntityType, Long relatedEntityId, Boolean isRead,
                       Boolean isEmailSent, Boolean isPushSent, String priority, String category,
                       String actionUrl, String actionText, LocalDateTime expiresAt,
                       LocalDateTime readAt, LocalDateTime emailSentAt, LocalDateTime pushSentAt,
                       Long senderId, String metadata, Boolean isSystemGenerated,
                       String groupKey, Integer retryCount, LocalDateTime lastRetryAt,
                       User user, User sender, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.isRead = isRead != null ? isRead : false;
        this.isEmailSent = isEmailSent != null ? isEmailSent : false;
        this.isPushSent = isPushSent != null ? isPushSent : false;
        this.priority = priority != null ? priority : "MEDIUM";
        this.category = category;
        this.actionUrl = actionUrl;
        this.actionText = actionText;
        this.expiresAt = expiresAt;
        this.readAt = readAt;
        this.emailSentAt = emailSentAt;
        this.pushSentAt = pushSentAt;
        this.senderId = senderId;
        this.metadata = metadata;
        this.isSystemGenerated = isSystemGenerated != null ? isSystemGenerated : true;
        this.groupKey = groupKey;
        this.retryCount = retryCount != null ? retryCount : 0;
        this.lastRetryAt = lastRetryAt;
        this.user = user;
        this.sender = sender;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Boolean getIsEmailSent() {
        return isEmailSent;
    }

    public void setIsEmailSent(Boolean isEmailSent) {
        this.isEmailSent = isEmailSent;
    }

    public Boolean getIsPushSent() {
        return isPushSent;
    }

    public void setIsPushSent(Boolean isPushSent) {
        this.isPushSent = isPushSent;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getActionText() {
        return actionText;
    }

    public void setActionText(String actionText) {
        this.actionText = actionText;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getEmailSentAt() {
        return emailSentAt;
    }

    public void setEmailSentAt(LocalDateTime emailSentAt) {
        this.emailSentAt = emailSentAt;
    }

    public LocalDateTime getPushSentAt() {
        return pushSentAt;
    }

    public void setPushSentAt(LocalDateTime pushSentAt) {
        this.pushSentAt = pushSentAt;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Boolean getIsSystemGenerated() {
        return isSystemGenerated;
    }

    public void setIsSystemGenerated(Boolean isSystemGenerated) {
        this.isSystemGenerated = isSystemGenerated;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getLastRetryAt() {
        return lastRetryAt;
    }

    public void setLastRetryAt(LocalDateTime lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUnread() {
        return !Boolean.TRUE.equals(isRead);
    }

    public boolean needsEmailNotification() {
        return !Boolean.TRUE.equals(isEmailSent) && !isExpired();
    }

    public boolean needsPushNotification() {
        return !Boolean.TRUE.equals(isPushSent) && !isExpired();
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void markEmailSent() {
        this.isEmailSent = true;
        this.emailSentAt = LocalDateTime.now();
    }

    public void markPushSent() {
        this.isPushSent = true;
        this.pushSentAt = LocalDateTime.now();
    }

    public boolean isHighPriority() {
        return "HIGH".equals(priority) || "URGENT".equals(priority);
    }

    public boolean isUrgent() {
        return "URGENT".equals(priority);
    }

    public boolean canRetry() {
        return (retryCount != null ? retryCount : 0) < 3 && !isExpired();
    }

    public void incrementRetryCount() {
        this.retryCount = (this.retryCount != null ? this.retryCount : 0) + 1;
        this.lastRetryAt = LocalDateTime.now();
    }

    public long getAgeInHours() {
        if (createdAt == null) return 0;
        return java.time.temporal.ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
    }

    public boolean isRecent() {
        return getAgeInHours() <= 24;
    }

    public void setActionButton(String url, String text) {
        this.actionUrl = url;
        this.actionText = text;
    }

    public void setExpiration(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setExpiration(int hours) {
        this.expiresAt = LocalDateTime.now().plusHours(hours);
    }

    private String determineCategoryFromType(NotificationType type) {
        if (type == null) return "GENERAL";
        
        if (type.isExpenseRelated()) return "EXPENSE";
        if (type.isBudgetRelated()) return "BUDGET";
        if (type.isApprovalRelated()) return "APPROVAL";
        if (type.isCommentRelated()) return "COMMENT";
        if (type.isSecurityRelated()) return "SECURITY";
        if (type.isSystemRelated()) return "SYSTEM";
        if (type.isAiRelated()) return "AI";
        return "GENERAL";
    }

    // Static factory methods
    public static Notification createExpenseNotification(Long userId, Long expenseId, 
                                                        NotificationType type, String title, String message) {
        Notification notification = new Notification(userId, type, title, message, "EXPENSE", expenseId);
        notification.setPriority(type.isUrgent() ? "URGENT" : type.requiresAction() ? "HIGH" : "MEDIUM");
        return notification;
    }

    public static Notification createApprovalNotification(Long userId, Long workflowId,
                                                         NotificationType type, String title, String message) {
        Notification notification = new Notification(userId, type, title, message, "APPROVAL", workflowId);
        notification.setPriority(type.requiresAction() ? "HIGH" : "MEDIUM");
        return notification;
    }

    public static Notification createBudgetNotification(Long userId, Long budgetId,
                                                       NotificationType type, String title, String message) {
        Notification notification = new Notification(userId, type, title, message, "BUDGET", budgetId);
        notification.setPriority(type == NotificationType.BUDGET_EXCEEDED ? "URGENT" : "MEDIUM");
        return notification;
    }

    public static Notification createCommentNotification(Long userId, Long commentId,
                                                        NotificationType type, String title, String message) {
        return new Notification(userId, type, title, message, "COMMENT", commentId);
    }

    public static Notification createSystemNotification(Long userId, NotificationType type, 
                                                       String title, String message) {
        Notification notification = new Notification(userId, type, title, message);
        notification.setCategory("SYSTEM");
        return notification;
    }

    // toString method for debugging
    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                ", priority='" + priority + '\'' +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // equals and hashCode methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}