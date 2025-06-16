-- V10__Create_comments_table.sql
-- Migration script to create the comments table for expense tracking system

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT,
    workflow_id BIGINT,
    user_id BIGINT NOT NULL,
    comment_text TEXT NOT NULL,
    comment_type VARCHAR(50) DEFAULT 'GENERAL',
    is_internal BOOLEAN DEFAULT FALSE,
    is_system_generated BOOLEAN DEFAULT FALSE,
    parent_comment_id BIGINT,
    mentioned_users VARCHAR(500),
    attachment_url VARCHAR(500),
    is_edited BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    visibility VARCHAR(20) DEFAULT 'ALL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_comments_expense 
        FOREIGN KEY (expense_id) REFERENCES expenses(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_comments_workflow 
        FOREIGN KEY (workflow_id) REFERENCES approval_workflows(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_comments_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_comments_parent 
        FOREIGN KEY (parent_comment_id) REFERENCES comments(id) 
        ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT chk_comment_type 
        CHECK (comment_type IN ('GENERAL', 'APPROVAL', 'REJECTION', 'ESCALATION', 'SYSTEM', 'QUESTION', 'CLARIFICATION')),
    
    CONSTRAINT chk_visibility 
        CHECK (visibility IN ('ALL', 'TEAM', 'APPROVERS', 'ADMIN')),
    
    CONSTRAINT chk_comment_text_length 
        CHECK (CHAR_LENGTH(comment_text) <= 2000),
    
    CONSTRAINT chk_attachment_url_length 
        CHECK (CHAR_LENGTH(attachment_url) <= 500),
    
    CONSTRAINT chk_mentioned_users_length 
        CHECK (CHAR_LENGTH(mentioned_users) <= 500),
    
    -- Ensure either expense_id or workflow_id is provided (but not necessarily both)
    CONSTRAINT chk_expense_or_workflow 
        CHECK (expense_id IS NOT NULL OR workflow_id IS NOT NULL)
);

-- Create indexes for better query performance
CREATE INDEX idx_comments_expense_id ON comments(expense_id);
CREATE INDEX idx_comments_workflow_id ON comments(workflow_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);
CREATE INDEX idx_comments_comment_type ON comments(comment_type);
CREATE INDEX idx_comments_visibility ON comments(visibility);
CREATE INDEX idx_comments_is_deleted ON comments(is_deleted);
CREATE INDEX idx_comments_is_system_generated ON comments(is_system_generated);
CREATE INDEX idx_comments_is_internal ON comments(is_internal);

-- Composite indexes for common query patterns
CREATE INDEX idx_comments_expense_not_deleted ON comments(expense_id, is_deleted, created_at);
CREATE INDEX idx_comments_workflow_not_deleted ON comments(workflow_id, is_deleted, created_at);
CREATE INDEX idx_comments_user_not_deleted ON comments(user_id, is_deleted, created_at);
CREATE INDEX idx_comments_type_not_deleted ON comments(comment_type, is_deleted, created_at);
CREATE INDEX idx_comments_visibility_not_deleted ON comments(visibility, is_deleted, created_at);