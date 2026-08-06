ALTER TABLE payments
    ADD COLUMN gateway_payment_id VARCHAR(255),
    ADD COLUMN gateway_order_id VARCHAR(255),
    ADD COLUMN refund_reference VARCHAR(255),
    ADD COLUMN refund_amount DECIMAL(10, 2),
    ADD COLUMN refund_status VARCHAR(50);
