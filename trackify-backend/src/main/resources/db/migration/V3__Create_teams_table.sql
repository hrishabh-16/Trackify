-- ===========================================
-- CREATE TEAMS TABLE
-- ===========================================

CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    owner_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_members INT DEFAULT 50,
    invite_code VARCHAR(50) UNIQUE,
    auto_approve_members BOOLEAN DEFAULT FALSE,
    currency VARCHAR(3) DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_teams_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes for better performance
    INDEX idx_teams_owner_id (owner_id),
    INDEX idx_teams_name (name),
    INDEX idx_teams_invite_code (invite_code),
    INDEX idx_teams_is_active (is_active),
    INDEX idx_teams_currency (currency),
    INDEX idx_teams_created_at (created_at),
    
    -- Unique constraint to prevent duplicate team names per owner
    UNIQUE KEY uk_teams_name_owner (name, owner_id)
);

-- Add comments for documentation
ALTER TABLE teams COMMENT = 'Teams table for organizing users into groups for expense tracking';
ALTER TABLE teams MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT 'Primary key for team';
ALTER TABLE teams MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT 'Team name';
ALTER TABLE teams MODIFY COLUMN description VARCHAR(500) COMMENT 'Team description';
ALTER TABLE teams MODIFY COLUMN owner_id BIGINT NOT NULL COMMENT 'User ID of team owner';
ALTER TABLE teams MODIFY COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether team is active';
ALTER TABLE teams MODIFY COLUMN max_members INT DEFAULT 50 COMMENT 'Maximum number of team members allowed';
ALTER TABLE teams MODIFY COLUMN invite_code VARCHAR(50) COMMENT 'Unique invite code for joining team';
ALTER TABLE teams MODIFY COLUMN auto_approve_members BOOLEAN DEFAULT FALSE COMMENT 'Whether to auto-approve new members';
ALTER TABLE teams MODIFY COLUMN currency VARCHAR(3) DEFAULT 'USD' COMMENT 'Default currency for team expenses';
ALTER TABLE teams MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when team was created';
ALTER TABLE teams MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Timestamp when team was last updated';