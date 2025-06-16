-- V8__Create_notifications_table.sql
-- Migration script to create the notifications table for expense tracking system

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    is_push_sent BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    category VARCHAR(50),
    action_url VARCHAR(500),
    action_text VARCHAR(100),
    expires_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    email_sent_at TIMESTAMP NULL,
    push_sent_at TIMESTAMP NULL,
    sender_id BIGINT,
    metadata TEXT,
    is_system_generated BOOLEAN NOT NULL DEFAULT TRUE,
    group_key VARCHAR(100),
    retry_count INT DEFAULT 0,
    last_retry_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_notifications_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_notifications_sender 
        FOREIGN KEY (sender_id) REFERENCES users(id) 
        ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_notification_type 
        CHECK (type IN ('EXPENSE_SUBMITTED', 'EXPENSE_APPROVED', 'EXPENSE_REJECTED', 'EXPENSE_ESCALATED', 
                       'EXPENSE_AUTO_APPROVED', 'EXPENSE_CANCELLED', 'EXPENSE_PENDING_APPROVAL', 'EXPENSE_OVERDUE',
                       'BUDGET_EXCEEDED', 'BUDGET_WARNING', 'BUDGET_CREATED', 'BUDGET_UPDATED', 'BUDGET_EXPIRED',
                       'COMMENT_ADDED', 'COMMENT_REPLY', 'COMMENT_MENTION',
                       'APPROVAL_REQUEST', 'APPROVAL_REMINDER', 'APPROVAL_ESCALATION',
                       'REPORT_GENERATED', 'REPORT_READY', 'SYSTEM_UPDATE',
                       'POLICY_VIOLATION', 'DUPLICATE_EXPENSE', 'RECEIPT_MISSING',
                       'ACCOUNT_LOCKED', 'PASSWORD_RESET', 'LOGIN_ALERT',
                       'AI_SUGGESTION', 'ANOMALY_DETECTED')),
    
    CONSTRAINT chk_priority 
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    
    CONSTRAINT chk_category 
        CHECK (category IN ('EXPENSE', 'BUDGET', 'APPROVAL', 'COMMENT', 'SYSTEM', 'SECURITY', 'AI', 'GENERAL')),
    
    CONSTRAINT chk_related_entity_type 
        CHECK (related_entity_type IN ('EXPENSE', 'BUDGET', 'APPROVAL', 'COMMENT', 'USER', 'TEAM', 'CATEGORY', 'REPORT')),
    
    CONSTRAINT chk_title_length 
        CHECK (CHAR_LENGTH(title) <= 200),
    
    CONSTRAINT chk_message_length 
        CHECK (CHAR_LENGTH(message) <= 1000),
    
    CONSTRAINT chk_action_url_length 
        CHECK (CHAR_LENGTH(action_url) <= 500),
    
    CONSTRAINT chk_action_text_length 
        CHECK (CHAR_LENGTH(action_text) <= 100),
    
    CONSTRAINT chk_group_key_length 
        CHECK (CHAR_LENGTH(group_key) <= 100),
    
    CONSTRAINT chk_retry_count_non_negative 
        CHECK (retry_count >= 0),
    
    CONSTRAINT chk_retry_count_limit 
        CHECK (retry_count <= 5),
    
    -- Business logic constraints
    CONSTRAINT chk_read_at_when_read 
        CHECK ((is_read = TRUE AND read_at IS NOT NULL) OR (is_read = FALSE)),
    
    CONSTRAINT chk_email_sent_at_when_sent 
        CHECK ((is_email_sent = TRUE AND email_sent_at IS NOT NULL) OR (is_email_sent = FALSE)),
    
    CONSTRAINT chk_push_sent_at_when_sent 
        CHECK ((is_push_sent = TRUE AND push_sent_at IS NOT NULL) OR (is_push_sent = FALSE)),
    
    CONSTRAINT chk_expires_at_future 
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

-- Create indexes for better query performance
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_sender_id ON notifications(sender_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_priority ON notifications(priority);
CREATE INDEX idx_notifications_category ON notifications(category);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_is_email_sent ON notifications(is_email_sent);
CREATE INDEX idx_notifications_is_push_sent ON notifications(is_push_sent);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_expires_at ON notifications(expires_at);
CREATE INDEX idx_notifications_read_at ON notifications(read_at);
CREATE INDEX idx_notifications_group_key ON notifications(group_key);
CREATE INDEX idx_notifications_related_entity ON notifications(related_entity_type, related_entity_id);
CREATE INDEX idx_notifications_is_system_generated ON notifications(is_system_generated);
CREATE INDEX idx_notifications_retry_count ON notifications(retry_count);

-- Composite indexes for common query patterns
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read, created_at);
CREATE INDEX idx_notifications_user_type ON notifications(user_id, type, created_at);
CREATE INDEX idx_notifications_user_category ON notifications(user_id, category, created_at);
CREATE INDEX idx_notifications_user_priority ON notifications(user_id, priority, created_at);
CREATE INDEX idx_notifications_user_read_created ON notifications(user_id, is_read, created_at);
CREATE INDEX idx_notifications_type_unread ON notifications(type, is_read, created_at);
CREATE INDEX idx_notifications_priority_unread ON notifications(priority, is_read, created_at);
CREATE INDEX idx_notifications_category_unread ON notifications(category, is_read, created_at);
CREATE INDEX idx_notifications_expired ON notifications(expires_at, is_read);
CREATE INDEX idx_notifications_delivery_status ON notifications(is_email_sent, is_push_sent, retry_count);
CREATE INDEX idx_notifications_retry_needed ON notifications(is_email_sent, is_push_sent, retry_count, last_retry_at);

-- Performance indexes for cleanup operations
CREATE INDEX idx_notifications_cleanup_expired ON notifications(expires_at, created_at);
CREATE INDEX idx_notifications_cleanup_old_read ON notifications(is_read, read_at, created_at);

-- Index for grouping operations
CREATE INDEX idx_notifications_group_user ON notifications(group_key, user_id, created_at);

-- Index for administrative queries
CREATE INDEX idx_notifications_admin_stats ON notifications(type, category, priority, created_at);

-- Update some sample notifications to show different states
UPDATE notifications SET is_read = TRUE, read_at = NOW() WHERE id = 2;
UPDATE notifications SET is_email_sent = TRUE, email_sent_at = NOW() WHERE id IN (1, 3);
UPDATE notifications SET expires_at = DATE_ADD(NOW(), INTERVAL 7 DAY) WHERE id = 3;