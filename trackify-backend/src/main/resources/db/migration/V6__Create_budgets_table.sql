-- ===========================================
-- CREATE BUDGETS TABLE
-- ===========================================

CREATE TABLE budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    total_amount DECIMAL(12,2) NOT NULL,
    spent_amount DECIMAL(12,2) DEFAULT 0.00,
    remaining_amount DECIMAL(12,2),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    alert_threshold DECIMAL(5,2) DEFAULT 80.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_period VARCHAR(20),
    user_id BIGINT NOT NULL,
    category_id BIGINT,
    team_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_budgets_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_budgets_total_amount_positive CHECK (total_amount > 0),
    CONSTRAINT chk_budgets_spent_amount_non_negative CHECK (spent_amount >= 0),
    CONSTRAINT chk_budgets_alert_threshold_range CHECK (alert_threshold >= 0 AND alert_threshold <= 100),
    CONSTRAINT chk_budgets_date_range CHECK (end_date >= start_date),
    CONSTRAINT chk_budgets_currency_format CHECK (currency REGEXP '^[A-Z]{3}$'),
    CONSTRAINT chk_budgets_recurrence_period CHECK (
        recurrence_period IS NULL OR 
        recurrence_period IN ('MONTHLY', 'QUARTERLY', 'YEARLY')
    ),
    
    -- Indexes for better performance
    INDEX idx_budgets_user_id (user_id),
    INDEX idx_budgets_category_id (category_id),
    INDEX idx_budgets_team_id (team_id),
    INDEX idx_budgets_is_active (is_active),
    INDEX idx_budgets_is_recurring (is_recurring),
    INDEX idx_budgets_start_date (start_date),
    INDEX idx_budgets_end_date (end_date),
    INDEX idx_budgets_date_range (start_date, end_date),
    INDEX idx_budgets_user_active (user_id, is_active),
    INDEX idx_budgets_user_category (user_id, category_id),
    INDEX idx_budgets_user_dates (user_id, start_date, end_date),
    INDEX idx_budgets_alert_threshold (alert_threshold),
    INDEX idx_budgets_spent_percentage ((spent_amount / total_amount * 100)),
    INDEX idx_budgets_created_at (created_at)
);

-- Add comments for documentation
ALTER TABLE budgets COMMENT = 'Budgets table for tracking spending limits and financial goals';
ALTER TABLE budgets MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT 'Primary key for budget';
ALTER TABLE budgets MODIFY COLUMN name VARCHAR(200) NOT NULL COMMENT 'Budget name';
ALTER TABLE budgets MODIFY COLUMN description VARCHAR(500) COMMENT 'Budget description';
ALTER TABLE budgets MODIFY COLUMN total_amount DECIMAL(12,2) NOT NULL COMMENT 'Total budget amount';
ALTER TABLE budgets MODIFY COLUMN spent_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT 'Amount spent so far';
ALTER TABLE budgets MODIFY COLUMN remaining_amount DECIMAL(12,2) COMMENT 'Remaining budget amount';
ALTER TABLE budgets MODIFY COLUMN start_date DATE NOT NULL COMMENT 'Budget start date';
ALTER TABLE budgets MODIFY COLUMN end_date DATE NOT NULL COMMENT 'Budget end date';
ALTER TABLE budgets MODIFY COLUMN currency VARCHAR(3) DEFAULT 'USD' COMMENT 'Budget currency code';
ALTER TABLE budgets MODIFY COLUMN alert_threshold DECIMAL(5,2) DEFAULT 80.00 COMMENT 'Alert threshold percentage';
ALTER TABLE budgets MODIFY COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether budget is active';
ALTER TABLE budgets MODIFY COLUMN is_recurring BOOLEAN DEFAULT FALSE COMMENT 'Whether budget recurs automatically';
ALTER TABLE budgets MODIFY COLUMN recurrence_period VARCHAR(20) COMMENT 'Recurrence period (MONTHLY, QUARTERLY, YEARLY)';
ALTER TABLE budgets MODIFY COLUMN user_id BIGINT NOT NULL COMMENT 'Reference to budget owner';
ALTER TABLE budgets MODIFY COLUMN category_id BIGINT COMMENT 'Reference to budget category';
ALTER TABLE budgets MODIFY COLUMN team_id BIGINT COMMENT 'Reference to team (for team budgets)';
ALTER TABLE budgets MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When budget was created';
ALTER TABLE budgets MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'When budget was last updated';

-- Add trigger to automatically calculate remaining_amount
DELIMITER //
CREATE TRIGGER trg_budgets_calculate_remaining
    BEFORE INSERT ON budgets
    FOR EACH ROW
BEGIN
    IF NEW.remaining_amount IS NULL THEN
        SET NEW.remaining_amount = NEW.total_amount - IFNULL(NEW.spent_amount, 0);
    END IF;
END//

CREATE TRIGGER trg_budgets_update_remaining
    BEFORE UPDATE ON budgets
    FOR EACH ROW
BEGIN
    SET NEW.remaining_amount = NEW.total_amount - IFNULL(NEW.spent_amount, 0);
END//
DELIMITER ;

-- Add some sample data (optional)
-- INSERT INTO budgets (name, description, total_amount, start_date, end_date, user_id, category_id) VALUES
-- ('Monthly Groceries', 'Monthly grocery budget', 500.00, '2024-01-01', '2024-01-31', 1, 1),
-- ('Quarterly Travel', 'Quarterly travel budget', 2000.00, '2024-01-01', '2024-03-31', 1, 2);