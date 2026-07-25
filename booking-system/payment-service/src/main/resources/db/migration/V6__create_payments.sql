-- Flyway Migration V6: Payment Service (Payments Aggregate)
-- Target Database: payment_db

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Function to handle automatic updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. Table: payments
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,       -- Logical FK to Booking Service
    user_id UUID NOT NULL,          -- Logical FK to Auth Service
    idempotency_key VARCHAR(255) NOT NULL,
    payment_gateway VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(255) NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_method VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    failure_reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED'))
);

CREATE TRIGGER trg_payments_updated_at
BEFORE UPDATE ON payments
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Indexes on payments table
CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);
CREATE INDEX idx_payments_txn_ref ON payments(transaction_reference) WHERE transaction_reference IS NOT NULL;
