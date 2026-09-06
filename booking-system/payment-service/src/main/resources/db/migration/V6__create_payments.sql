-- Flyway Migration V6: Payment Service (Payments Aggregate)
-- Target Database: vibecheck_payment (MySQL 8.0)

-- 1. Table: payments
CREATE TABLE payments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    booking_id BINARY(16) NOT NULL,       -- Logical FK to Booking Service
    user_id BINARY(16) NOT NULL,          -- Logical FK to Auth Service
    idempotency_key VARCHAR(255) NOT NULL,
    payment_gateway VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(255) NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_method VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    failure_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes on payments table
CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);
CREATE INDEX idx_payments_txn_ref ON payments(transaction_reference);
