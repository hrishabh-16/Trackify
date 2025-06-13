-- ===========================================
-- CREATE TEAM_MEMBERS TABLE
-- ===========================================

CREATE TABLE team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('OWNER', 'ADMIN', 'MANAGER', 'MEMBER', 'VIEWER') NOT NULL DEFAULT 'MEMBER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    invited_by BIGINT,
    invitation_accepted_at TIMESTAMP NULL,
    invitation_expires_at TIMESTAMP NULL,
    last_active_at TIMESTAMP NULL,
    notes VARCHAR(500),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_invited_by FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Unique constraint to prevent duplicate memberships
    UNIQUE KEY uk_team_members_team_user (team_id, user_id),
    
    -- Indexes for better performance
    INDEX idx_team_members_team_id (team_id),
    INDEX idx_team_members_user_id (user_id),
    INDEX idx_team_members_role (role),
    INDEX idx_team_members_is_active (is_active),
    INDEX idx_team_members_invited_by (invited_by),
    INDEX idx_team_members_invitation_expires_at (invitation_expires_at),
    INDEX idx_team_members_last_active_at (last_active_at),
    INDEX idx_team_members_joined_at (joined_at),
    INDEX idx_team_members_team_active (team_id, is_active),
    INDEX idx_team_members_user_active (user_id, is_active),
    INDEX idx_team_members_team_role (team_id, role),
    INDEX idx_team_members_pending_invitations (is_active, invitation_expires_at)
);

-- Add comments for documentation
ALTER TABLE team_members COMMENT = 'Team membership table linking users to teams with roles and permissions';
ALTER TABLE team_members MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT 'Primary key for team membership';
ALTER TABLE team_members MODIFY COLUMN team_id BIGINT NOT NULL COMMENT 'Reference to team';
ALTER TABLE team_members MODIFY COLUMN user_id BIGINT NOT NULL COMMENT 'Reference to user';
ALTER TABLE team_members MODIFY COLUMN role ENUM('OWNER', 'ADMIN', 'MANAGER', 'MEMBER', 'VIEWER') NOT NULL DEFAULT 'MEMBER' COMMENT 'Role of user in team';
ALTER TABLE team_members MODIFY COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether membership is active';
ALTER TABLE team_members MODIFY COLUMN invited_by BIGINT COMMENT 'User ID who sent the invitation';
ALTER TABLE team_members MODIFY COLUMN invitation_accepted_at TIMESTAMP NULL COMMENT 'When invitation was accepted';
ALTER TABLE team_members MODIFY COLUMN invitation_expires_at TIMESTAMP NULL COMMENT 'When invitation expires';
ALTER TABLE team_members MODIFY COLUMN last_active_at TIMESTAMP NULL COMMENT 'Last activity timestamp for member';
ALTER TABLE team_members MODIFY COLUMN notes VARCHAR(500) COMMENT 'Additional notes about membership';
ALTER TABLE team_members MODIFY COLUMN joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When user joined team';
ALTER TABLE team_members MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'When membership was last updated';

-- Add check constraints for data integrity
ALTER TABLE team_members ADD CONSTRAINT chk_team_members_invitation_logic 
    CHECK (
        (is_active = TRUE AND invitation_expires_at IS NULL) OR 
        (is_active = FALSE AND invitation_expires_at IS NOT NULL)
    );

-- Add trigger to automatically update last_active_at when is_active changes to TRUE
DELIMITER //
CREATE TRIGGER trg_team_members_update_last_active
    BEFORE UPDATE ON team_members
    FOR EACH ROW
BEGIN
    IF NEW.is_active = TRUE AND OLD.is_active = FALSE THEN
        SET NEW.last_active_at = CURRENT_TIMESTAMP;
    END IF;
END//
DELIMITER ;