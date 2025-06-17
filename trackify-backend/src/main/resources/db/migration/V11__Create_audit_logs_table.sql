-- ===========================================
-- CREATE AUDIT_LOGS TABLE
-- ===========================================
-- Migration script to create the audit_logs table for comprehensive audit logging

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    description VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    old_values TEXT,
    new_values TEXT,
    session_id VARCHAR(100),
    request_id VARCHAR(100),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(1000),
    execution_time_ms BIGINT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    team_id BIGINT,
    additional_data JSON,
    
    -- Foreign key constraints (optional - allows for system/anonymous actions)
    CONSTRAINT fk_audit_logs_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_audit_logs_team 
        FOREIGN KEY (team_id) REFERENCES teams(id) 
        ON DELETE SET NULL,
    
    -- Check constraints for data integrity
    CONSTRAINT chk_audit_action 
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'LOGIN_FAILED', 
                         'EXPORT', 'IMPORT', 'APPROVE', 'REJECT', 'SUBMIT', 'VIEW', 
                         'DOWNLOAD', 'UPLOAD', 'PASSWORD_CHANGE', 'PASSWORD_RESET', 
                         'PROFILE_UPDATE', 'SETTINGS_UPDATE')),
    
    CONSTRAINT chk_audit_entity_type 
        CHECK (entity_type IN ('USER', 'EXPENSE', 'CATEGORY', 'TEAM', 'BUDGET', 'RECEIPT', 
                              'NOTIFICATION', 'COMMENT', 'APPROVAL_WORKFLOW', 'REPORT', 'SYSTEM')),
    
    CONSTRAINT chk_audit_execution_time_non_negative 
        CHECK (execution_time_ms IS NULL OR execution_time_ms >= 0),
    
    CONSTRAINT chk_audit_description_length 
        CHECK (CHAR_LENGTH(description) <= 500),
    
    CONSTRAINT chk_audit_error_message_length 
        CHECK (CHAR_LENGTH(error_message) <= 1000),
    
    CONSTRAINT chk_audit_username_length 
        CHECK (CHAR_LENGTH(username) <= 100),
    
    CONSTRAINT chk_audit_session_id_length 
        CHECK (CHAR_LENGTH(session_id) <= 100),
    
    CONSTRAINT chk_audit_request_id_length 
        CHECK (CHAR_LENGTH(request_id) <= 100),
    
    CONSTRAINT chk_audit_user_agent_length 
        CHECK (CHAR_LENGTH(user_agent) <= 500)
);

-- ===========================================
-- CREATE INDEXES FOR PERFORMANCE
-- ===========================================

-- Primary indexes for frequent queries
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_username ON audit_logs(username);
CREATE INDEX idx_audit_success ON audit_logs(success);
CREATE INDEX idx_audit_team_id ON audit_logs(team_id);
CREATE INDEX idx_audit_session_id ON audit_logs(session_id);
CREATE INDEX idx_audit_request_id ON audit_logs(request_id);
CREATE INDEX idx_audit_ip_address ON audit_logs(ip_address);

-- Composite indexes for common query patterns
CREATE INDEX idx_audit_user_action ON audit_logs(user_id, action, timestamp);
CREATE INDEX idx_audit_user_entity ON audit_logs(user_id, entity_type, timestamp);
CREATE INDEX idx_audit_entity_action ON audit_logs(entity_type, action, timestamp);
CREATE INDEX idx_audit_entity_specific ON audit_logs(entity_type, entity_id, timestamp);
CREATE INDEX idx_audit_user_timestamp ON audit_logs(user_id, timestamp);
CREATE INDEX idx_audit_action_timestamp ON audit_logs(action, timestamp);
CREATE INDEX idx_audit_success_timestamp ON audit_logs(success, timestamp);
CREATE INDEX idx_audit_team_timestamp ON audit_logs(team_id, timestamp);

-- Specialized indexes for security and monitoring (without WHERE clause)
CREATE INDEX idx_audit_failed_actions ON audit_logs(success, action, timestamp);
CREATE INDEX idx_audit_login_attempts ON audit_logs(action, ip_address, timestamp);
CREATE INDEX idx_audit_user_activity ON audit_logs(user_id, action, timestamp);
CREATE INDEX idx_audit_sensitive_actions ON audit_logs(action, user_id, timestamp);

-- Performance indexes for analytics and reporting
CREATE INDEX idx_audit_daily_activity ON audit_logs(timestamp, action);
CREATE INDEX idx_audit_user_daily ON audit_logs(user_id, timestamp);
CREATE INDEX idx_audit_entity_daily ON audit_logs(entity_type, timestamp);
CREATE INDEX idx_audit_team_activity ON audit_logs(team_id, action, timestamp);

-- Indexes for cleanup and maintenance operations
CREATE INDEX idx_audit_timestamp_cleanup ON audit_logs(timestamp, success);

-- ===========================================
-- ADD TABLE COMMENTS FOR DOCUMENTATION
-- ===========================================

ALTER TABLE audit_logs COMMENT = 'Comprehensive audit log table for tracking all system activities and changes';

-- ===========================================
-- CREATE TRIGGERS FOR AUTOMATIC CLEANUP
-- ===========================================

-- Trigger to prevent updates to audit logs (immutable records)
DELIMITER //
CREATE TRIGGER trg_audit_logs_prevent_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Audit log records are immutable and cannot be updated';
END//
DELIMITER ;

-- ===========================================
-- CREATE PROCEDURES FOR MAINTENANCE
-- ===========================================

-- Procedure to clean up old audit logs
DELIMITER //
CREATE PROCEDURE sp_cleanup_old_audit_logs(IN days_to_keep INT)
BEGIN
    DECLARE deleted_count INT DEFAULT 0;
    DECLARE cutoff_date TIMESTAMP;
    
    SET cutoff_date = DATE_SUB(NOW(), INTERVAL days_to_keep DAY);
    
    -- Delete old audit logs except for critical actions
    DELETE FROM audit_logs 
    WHERE timestamp < cutoff_date 
    AND action NOT IN ('LOGIN_FAILED', 'PASSWORD_CHANGE', 'DELETE', 'SETTINGS_UPDATE');
    
    SET deleted_count = ROW_COUNT();
    
    -- Log the cleanup action
    INSERT INTO audit_logs (username, action, entity_type, description, success)
    VALUES ('SYSTEM', 'DELETE', 'SYSTEM', 
            CONCAT('Cleaned up ', deleted_count, ' old audit log records older than ', days_to_keep, ' days'), 
            TRUE);
    
    SELECT CONCAT('Deleted ', deleted_count, ' old audit log records') AS result;
END//
DELIMITER ;

-- Procedure to get audit statistics
DELIMITER //
CREATE PROCEDURE sp_audit_statistics(IN from_date DATE, IN to_date DATE)
BEGIN
    -- Summary statistics
    SELECT 
        'Total Actions' as metric,
        COUNT(*) as value
    FROM audit_logs 
    WHERE DATE(timestamp) BETWEEN from_date AND to_date
    
    UNION ALL
    
    SELECT 
        'Failed Actions' as metric,
        COUNT(*) as value
    FROM audit_logs 
    WHERE DATE(timestamp) BETWEEN from_date AND to_date 
    AND success = FALSE
    
    UNION ALL
    
    SELECT 
        'Unique Users' as metric,
        COUNT(DISTINCT user_id) as value
    FROM audit_logs 
    WHERE DATE(timestamp) BETWEEN from_date AND to_date 
    AND user_id IS NOT NULL
    
    UNION ALL
    
    SELECT 
        'Login Attempts' as metric,
        COUNT(*) as value
    FROM audit_logs 
    WHERE DATE(timestamp) BETWEEN from_date AND to_date 
    AND action IN ('LOGIN', 'LOGIN_FAILED');
    
    -- Action breakdown
    SELECT 
        'Action Breakdown' as section,
        action,
        COUNT(*) as count,
        COUNT(CASE WHEN success = FALSE THEN 1 END) as failed_count,
        ROUND(AVG(execution_time_ms), 2) as avg_execution_time_ms
    FROM audit_logs 
    WHERE DATE(timestamp) BETWEEN from_date AND to_date
    GROUP BY action
    ORDER BY count DESC;
    
    -- Entity type breakdown
    SELECT 
        'Entity Breakdown' as section,
        entity_type,
        COUNT(*) as count
    FROM audit_logs 
    WHERE DATE(timestamp) BETWEEN from_date AND to_date
    GROUP BY entity_type
    ORDER BY count DESC;
END//
DELIMITER ;

-- ===========================================
-- INITIAL SYSTEM LOG ENTRY
-- ===========================================

-- Insert initial audit log entry to mark table creation
INSERT INTO audit_logs (
    username, 
    action, 
    entity_type, 
    description, 
    success,
    additional_data
) VALUES (
    'SYSTEM', 
    'CREATE', 
    'SYSTEM', 
    'Audit logs table created and initialized', 
    TRUE,
    '{"migration": "V11__Create_audit_logs_table.sql", "version": "1.0.0"}'
);