-- Flyway Migration V6: Create Outbox Events and Processed Events tables for Kafka Event Layer
-- Target Database: booking_db

-- 1. Table: outbox_events (Transactional Outbox Pattern)
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ NULL,
    error_message TEXT NULL
);

CREATE INDEX idx_outbox_events_status ON outbox_events(status, created_at) WHERE status = 'PENDING';
CREATE INDEX idx_outbox_events_aggregate ON outbox_events(aggregate_type, aggregate_id);

-- 2. Table: processed_events (Consumer Idempotency Store)
CREATE TABLE processed_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    consumer_group VARCHAR(100) NOT NULL DEFAULT 'booking-service-group',
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_events_type ON processed_events(event_type);
