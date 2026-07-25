-- Flyway Migration V5: Booking Service (Bookings and BookingSeats Aggregate)
-- Target Database: booking_db

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Function to handle automatic updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. Table: bookings
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_reference VARCHAR(12) NOT NULL,
    user_id UUID NOT NULL,       -- Logical FK to Auth Service
    show_id UUID NOT NULL,       -- Logical FK to Show Service
    total_amount NUMERIC(10, 2) NOT NULL,
    tax_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    convenience_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_bookings_reference UNIQUE (booking_reference),
    CONSTRAINT chk_bookings_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED'))
);

CREATE TRIGGER trg_bookings_updated_at
BEFORE UPDATE ON bookings
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Indexes on bookings table
CREATE INDEX idx_bookings_user_id ON bookings(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_bookings_show_id ON bookings(show_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_bookings_status_expiry ON bookings(status, expires_at) WHERE status = 'PENDING';
CREATE INDEX idx_bookings_reference ON bookings(booking_reference);

-- 2. Table: booking_seats
CREATE TABLE booking_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    show_seat_id UUID NOT NULL,   -- Logical FK to Show Service (show_seats.id)
    seat_number VARCHAR(20) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT uk_booking_seats_booking_showseat UNIQUE (booking_id, show_seat_id),
    CONSTRAINT chk_booking_seats_price CHECK (price >= 0)
);

CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);
