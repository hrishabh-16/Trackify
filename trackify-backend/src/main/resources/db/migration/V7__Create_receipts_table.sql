-- V7__Create_receipts_table.sql

CREATE TABLE receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100),
    file_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    is_processed BOOLEAN DEFAULT FALSE,
    ocr_text TEXT,
    extracted_data JSON,
    expense_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_receipt_expense (expense_id),
    INDEX idx_receipt_user (uploaded_by),
    INDEX idx_receipt_filename (stored_filename),
    INDEX idx_receipt_processed (is_processed),
    INDEX idx_receipt_created (created_at),
    
    FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);