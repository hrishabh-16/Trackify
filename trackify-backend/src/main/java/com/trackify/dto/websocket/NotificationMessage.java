package com.trackify.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    
    private String title;
    private String message;
    private String type; // INFO, SUCCESS, WARNING, ERROR
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private String category; // EXPENSE, BUDGET, APPROVAL, SYSTEM
    private Long userId;
    private String username;
    private Long relatedEntityId;
    private String relatedEntityType;
    private String actionUrl;
    private boolean read;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiryTime;
    
    private Map<String, Object> additionalData;

    // Constructor for simple notification
    public NotificationMessage(String title, String message, String type, LocalDateTime timestamp) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.priority = "MEDIUM";
        this.read = false;
    }

    // Constructor for user-specific notification
    public NotificationMessage(String title, String message, String type, 
                             String username, LocalDateTime timestamp) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.username = username;
        this.timestamp = timestamp;
        this.priority = "MEDIUM";
        this.read = false;
    }

    // Constructor for expense-related notification
    public NotificationMessage(String title, String message, String type, 
                             String username, Long relatedEntityId, 
                             String relatedEntityType, LocalDateTime timestamp) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.username = username;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
        this.timestamp = timestamp;
        this.category = "EXPENSE";
        this.priority = "MEDIUM";
        this.read = false;
    }

    // Constructor for budget alert
    public NotificationMessage(String title, String message, String type, 
                             String priority, String username, LocalDateTime timestamp) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority;
        this.username = username;
        this.timestamp = timestamp;
        this.category = "BUDGET";
        this.read = false;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Long getRelatedEntityId() {
		return relatedEntityId;
	}

	public void setRelatedEntityId(Long relatedEntityId) {
		this.relatedEntityId = relatedEntityId;
	}

	public String getRelatedEntityType() {
		return relatedEntityType;
	}

	public void setRelatedEntityType(String relatedEntityType) {
		this.relatedEntityType = relatedEntityType;
	}

	public String getActionUrl() {
		return actionUrl;
	}

	public void setActionUrl(String actionUrl) {
		this.actionUrl = actionUrl;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public LocalDateTime getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(LocalDateTime expiryTime) {
		this.expiryTime = expiryTime;
	}

	public Map<String, Object> getAdditionalData() {
		return additionalData;
	}

	public void setAdditionalData(Map<String, Object> additionalData) {
		this.additionalData = additionalData;
	}
    
    
}