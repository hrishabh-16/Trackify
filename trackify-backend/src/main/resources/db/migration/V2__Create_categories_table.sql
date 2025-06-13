CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(7) DEFAULT '#6C757D',
    icon VARCHAR(50) DEFAULT '📁',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    team_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for better performance
    INDEX idx_categories_created_by (created_by),
    INDEX idx_categories_team_id (team_id),
    INDEX idx_categories_is_active (is_active),
    INDEX idx_categories_is_system (is_system),
    INDEX idx_categories_sort_order (sort_order),
    INDEX idx_categories_name (name),
    
    -- Composite indexes for common queries
    INDEX idx_categories_user_personal (created_by, team_id, sort_order),
    INDEX idx_categories_team (team_id, sort_order),
    INDEX idx_categories_active_user (is_active, created_by),
    
    -- Foreign key constraints (will be added when user and team tables exist)
    CONSTRAINT fk_categories_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
    -- CONSTRAINT fk_categories_team_id FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);

-- Add unique constraints to prevent duplicate category names
ALTER TABLE categories 
ADD CONSTRAINT uk_categories_name_user 
UNIQUE (name, created_by, team_id);

-- Add check constraints
ALTER TABLE categories 
ADD CONSTRAINT chk_categories_color 
CHECK (color REGEXP '^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$');

ALTER TABLE categories 
ADD CONSTRAINT chk_categories_sort_order 
CHECK (sort_order >= 0);