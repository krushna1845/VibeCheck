-- Flyway Migration V9: Create Processed Events table for Consumer Idempotency
-- Target Database: vibecheck_notification (MySQL 8.0)

CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(100) NOT NULL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL DEFAULT 'notification-service-group',
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_processed_events_type ON processed_events(event_type);
CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);
