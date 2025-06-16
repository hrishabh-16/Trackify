package com.trackify.entity;

import com.trackify.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String relatedEntityType; // EXPENSE, BUDGET, APPROVAL, COMMENT, etc.

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_email_sent", nullable = false)
    private Boolean isEmailSent = false;

    @Column(name = "is_push_sent", nullable = false)
    private Boolean isPushSent = false;

    @Column(name = "priority", length = 20)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "category", length = 50)
    private String category; // EXPENSE, BUDGET, APPROVAL, SYSTEM, SECURITY

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
    private String metadata; // JSON string for additional data

    @Column(name = "is_system_generated", nullable = false)
    private Boolean isSystemGenerated = true;

    @Column(name = "group_key", length = 100)
    private String groupKey; // For grouping related notifications

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private User sender;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Notification(Long userId, NotificationType type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.isEmailSent = false;
        this.isPushSent = false;
        this.priority = "MEDIUM";
        this.isSystemGenerated = true;
        this.retryCount = 0;
    }
    
    
    
    
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




	public Notification(Long userId, NotificationType type, String title, String message, 
                       String relatedEntityType, Long relatedEntityId) {
        this(userId, type, title, message);
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.category = determineCategoryFromType(type);
    }

    public Notification(Long userId, NotificationType type, String title, String message,
                       String relatedEntityType, Long relatedEntityId, String priority) {
        this(userId, type, title, message, relatedEntityType, relatedEntityId);
        this.priority = priority;
    }

    // Utility methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUnread() {
        return !isRead;
    }

    public boolean needsEmailNotification() {
        return !isEmailSent && !isExpired();
    }

    public boolean needsPushNotification() {
        return !isPushSent && !isExpired();
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
        return retryCount < 3 && !isExpired();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    public long getAgeInHours() {
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
}