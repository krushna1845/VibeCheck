CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    template_key VARCHAR(100) NOT NULL,
    subject VARCHAR(200),
    content TEXT,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
