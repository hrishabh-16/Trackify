package com.trackify.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketResponse {
    
    private String type; // MESSAGE_TYPE (EXPENSE_CREATED, NOTIFICATION, ERROR, etc.)
    private Object payload;
    private String status; // SUCCESS, ERROR, INFO
    private String message;
    private boolean success;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String requestId;
    private String sessionId;
    private String username;

    // Constructor for successful response
    public WebSocketResponse(String type, Object payload, LocalDateTime timestamp, boolean success) {
        this.type = type;
        this.payload = payload;
        this.timestamp = timestamp;
        this.success = success;
        this.status = success ? "SUCCESS" : "ERROR";
    }

    // Constructor for error response
    public WebSocketResponse(String type, String message, LocalDateTime timestamp, boolean success) {
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
        this.success = success;
        this.status = success ? "SUCCESS" : "ERROR";
    }

    // Constructor with full details
    public WebSocketResponse(String type, Object payload, String status, 
                           String message, boolean success, LocalDateTime timestamp) {
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.message = message;
        this.success = success;
        this.timestamp = timestamp;
    }

    // Constructor for user-specific response
    public WebSocketResponse(String type, Object payload, String username, 
                           LocalDateTime timestamp, boolean success) {
        this.type = type;
        this.payload = payload;
        this.username = username;
        this.timestamp = timestamp;
        this.success = success;
        this.status = success ? "SUCCESS" : "ERROR";
    }

    // Static method for success response
    public static WebSocketResponse success(String type, Object payload) {
        return new WebSocketResponse(type, payload, LocalDateTime.now(), true);
    }

    // Static method for error response
    public static WebSocketResponse error(String type, String message) {
        return new WebSocketResponse(type, message, LocalDateTime.now(), false);
    }

    // Static method for info response
    public static WebSocketResponse info(String type, Object payload, String message) {
        return new WebSocketResponse(type, payload, "INFO", message, true, LocalDateTime.now());
    }

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
    
    
}