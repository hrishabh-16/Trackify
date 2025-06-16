-- V9__Create_approval_workflows_table.sql
-- Migration script to create the approval_workflows table for expense tracking system

CREATE TABLE approval_workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,
    current_approver BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approval_level INT NOT NULL DEFAULT 1,
    max_approval_level INT NOT NULL DEFAULT 1,
    expense_amount DECIMAL(12, 2),
    approval_required_amount DECIMAL(12, 2),
    auto_approve_enabled BOOLEAN DEFAULT FALSE,
    escalation_enabled BOOLEAN DEFAULT FALSE,
    escalation_level INT DEFAULT 0,
    escalated_to BIGINT,
    escalated_at TIMESTAMP NULL,
    team_id BIGINT,
    category_id BIGINT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL,
    final_approver BIGINT,
    rejection_reason VARCHAR(1000),
    approval_notes VARCHAR(1000),
    deadline TIMESTAMP NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_approval_workflows_expense 
        FOREIGN KEY (expense_id) REFERENCES expenses(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_approval_workflows_submitted_by 
        FOREIGN KEY (submitted_by) REFERENCES users(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_approval_workflows_current_approver 
        FOREIGN KEY (current_approver) REFERENCES users(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_approval_workflows_final_approver 
        FOREIGN KEY (final_approver) REFERENCES users(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_approval_workflows_escalated_to 
        FOREIGN KEY (escalated_to) REFERENCES users(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_approval_workflows_team 
        FOREIGN KEY (team_id) REFERENCES teams(id) 
        ON DELETE SET NULL,
    
    CONSTRAINT fk_approval_workflows_category 
        FOREIGN KEY (category_id) REFERENCES categories(id) 
        ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_approval_status 
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'ESCALATED', 'AUTO_APPROVED')),
    
   CONSTRAINT chk_approval_workflows_priority 
    CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    
    CONSTRAINT chk_approval_level_positive 
        CHECK (approval_level > 0),
    
    CONSTRAINT chk_max_approval_level_positive 
        CHECK (max_approval_level > 0),
    
    CONSTRAINT chk_approval_level_within_max 
        CHECK (approval_level <= max_approval_level),
    
    CONSTRAINT chk_escalation_level_non_negative 
        CHECK (escalation_level >= 0),
    
    CONSTRAINT chk_expense_amount_non_negative 
        CHECK (expense_amount >= 0),
    
    CONSTRAINT chk_approval_required_amount_non_negative 
        CHECK (approval_required_amount >= 0),
    
    CONSTRAINT chk_rejection_reason_length 
        CHECK (CHAR_LENGTH(rejection_reason) <= 1000),
    
    CONSTRAINT chk_approval_notes_length 
        CHECK (CHAR_LENGTH(approval_notes) <= 1000)
);

-- Create indexes for better query performance
CREATE INDEX idx_approval_workflows_expense_id ON approval_workflows(expense_id);
CREATE INDEX idx_approval_workflows_submitted_by ON approval_workflows(submitted_by);
CREATE INDEX idx_approval_workflows_current_approver ON approval_workflows(current_approver);
CREATE INDEX idx_approval_workflows_final_approver ON approval_workflows(final_approver);
CREATE INDEX idx_approval_workflows_status ON approval_workflows(status);
CREATE INDEX idx_approval_workflows_team_id ON approval_workflows(team_id);
CREATE INDEX idx_approval_workflows_category_id ON approval_workflows(category_id);
CREATE INDEX idx_approval_workflows_priority ON approval_workflows(priority);
CREATE INDEX idx_approval_workflows_submitted_at ON approval_workflows(submitted_at);
CREATE INDEX idx_approval_workflows_approved_at ON approval_workflows(approved_at);
CREATE INDEX idx_approval_workflows_rejected_at ON approval_workflows(rejected_at);
CREATE INDEX idx_approval_workflows_deadline ON approval_workflows(deadline);
CREATE INDEX idx_approval_workflows_escalated_to ON approval_workflows(escalated_to);
CREATE INDEX idx_approval_workflows_escalation_level ON approval_workflows(escalation_level);

-- Composite indexes for common query patterns
CREATE INDEX idx_approval_workflows_status_approver ON approval_workflows(status, current_approver);
CREATE INDEX idx_approval_workflows_status_submitter ON approval_workflows(status, submitted_by);
CREATE INDEX idx_approval_workflows_status_team ON approval_workflows(status, team_id);
CREATE INDEX idx_approval_workflows_status_submitted_at ON approval_workflows(status, submitted_at);
CREATE INDEX idx_approval_workflows_deadline_status ON approval_workflows(deadline, status);
CREATE INDEX idx_approval_workflows_escalation_status ON approval_workflows(escalation_enabled, status);
CREATE INDEX idx_approval_workflows_amount_status ON approval_workflows(expense_amount, status);
CREATE INDEX idx_approval_workflows_priority_status ON approval_workflows(priority, status);

-- Unique constraint to ensure one workflow per expense
CREATE UNIQUE INDEX idx_approval_workflows_expense_unique ON approval_workflows(expense_id);