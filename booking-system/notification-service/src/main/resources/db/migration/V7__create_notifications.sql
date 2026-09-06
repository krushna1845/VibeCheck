-- Flyway Migration V7: Create notifications table
-- Target Database: vibecheck_notification (MySQL 8.0)

CREATE TABLE IF NOT EXISTS notifications (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    template_key VARCHAR(100) NOT NULL,
    subject VARCHAR(200) NULL,
    content TEXT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    metadata TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
