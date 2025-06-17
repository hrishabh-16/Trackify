package com.trackify.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "success", nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "additional_data", columnDefinition = "JSON")
    private String additionalData;

    // Audit Actions Constants
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_IMPORT = "IMPORT";
    public static final String ACTION_APPROVE = "APPROVE";
    public static final String ACTION_REJECT = "REJECT";
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_DOWNLOAD = "DOWNLOAD";
    public static final String ACTION_UPLOAD = "UPLOAD";
    public static final String ACTION_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String ACTION_PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String ACTION_SETTINGS_UPDATE = "SETTINGS_UPDATE";

    // Entity Types Constants
    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_EXPENSE = "EXPENSE";
    public static final String ENTITY_CATEGORY = "CATEGORY";
    public static final String ENTITY_TEAM = "TEAM";
    public static final String ENTITY_BUDGET = "BUDGET";
    public static final String ENTITY_RECEIPT = "RECEIPT";
    public static final String ENTITY_NOTIFICATION = "NOTIFICATION";
    public static final String ENTITY_COMMENT = "COMMENT";
    public static final String ENTITY_APPROVAL_WORKFLOW = "APPROVAL_WORKFLOW";
    public static final String ENTITY_REPORT = "REPORT";
    public static final String ENTITY_SYSTEM = "SYSTEM";

    // Manual Builder Implementation
    public static class Builder {
        private AuditLog auditLog;

        public Builder() {
            this.auditLog = new AuditLog();
            this.auditLog.success = true; // Default value
        }

        public Builder userId(Long userId) {
            this.auditLog.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.auditLog.username = username;
            return this;
        }

        public Builder action(String action) {
            this.auditLog.action = action;
            return this;
        }

        public Builder entityType(String entityType) {
            this.auditLog.entityType = entityType;
            return this;
        }

        public Builder entityId(Long entityId) {
            this.auditLog.entityId = entityId;
            return this;
        }

        public Builder description(String description) {
            this.auditLog.description = description;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.auditLog.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.auditLog.userAgent = userAgent;
            return this;
        }

        public Builder oldValues(String oldValues) {
            this.auditLog.oldValues = oldValues;
            return this;
        }

        public Builder newValues(String newValues) {
            this.auditLog.newValues = newValues;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.auditLog.sessionId = sessionId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.auditLog.requestId = requestId;
            return this;
        }

        public Builder success(Boolean success) {
            this.auditLog.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.auditLog.errorMessage = errorMessage;
            return this;
        }

        public Builder executionTimeMs(Long executionTimeMs) {
            this.auditLog.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder teamId(Long teamId) {
            this.auditLog.teamId = teamId;
            return this;
        }

        public Builder additionalData(String additionalData) {
            this.auditLog.additionalData = additionalData;
            return this;
        }

        public AuditLog build() {
            return this.auditLog;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Convenience methods for creating audit logs
    public static AuditLog create(Long userId, String username, String action, 
                                 String entityType, Long entityId, String description) {
        return AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .success(true)
                .build();
    }

    public static AuditLog createError(Long userId, String username, String action, 
                                      String entityType, String errorMessage) {
        return AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .entityType(entityType)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    public static AuditLog createSystemLog(String action, String description) {
        return AuditLog.builder()
                .username("SYSTEM")
                .action(action)
                .entityType(ENTITY_SYSTEM)
                .description(description)
                .success(true)
                .build();
    }

    // Utility method to set request context
    public AuditLog withRequestContext(String ipAddress, String userAgent, 
                                      String sessionId, String requestId) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionId = sessionId;
        this.requestId = requestId;
        return this;
    }

    // Utility method to set execution time
    public AuditLog withExecutionTime(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        return this;
    }

    // Utility method to set values for update operations
    public AuditLog withValues(String oldValues, String newValues) {
        this.oldValues = oldValues;
        this.newValues = newValues;
        return this;
    }

    // Utility method to set team context
    public AuditLog withTeamContext(Long teamId) {
        this.teamId = teamId;
        return this;
    }

    // Utility method to set additional data
    public AuditLog withAdditionalData(String additionalData) {
        this.additionalData = additionalData;
        return this;
    }
}