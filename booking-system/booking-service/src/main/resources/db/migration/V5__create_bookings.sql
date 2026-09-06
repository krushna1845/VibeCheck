-- Flyway Migration V5: Booking Service (Bookings and BookingSeats Aggregate)
-- Target Database: vibecheck_booking (MySQL 8.0)

-- 1. Table: bookings
CREATE TABLE bookings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    booking_reference VARCHAR(12) NOT NULL,
    user_id BINARY(16) NOT NULL,       -- Logical FK to Auth Service
    show_id BINARY(16) NOT NULL,       -- Logical FK to Show Service
    total_amount DECIMAL(10, 2) NOT NULL,
    tax_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    convenience_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_bookings_reference UNIQUE (booking_reference),
    CONSTRAINT chk_bookings_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_bookings_status CHECK (status IN ('CREATED', 'SEATS_LOCKED', 'PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes on bookings table
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_show_id ON bookings(show_id);
CREATE INDEX idx_bookings_status_expiry ON bookings(status, expires_at);
CREATE INDEX idx_bookings_reference ON bookings(booking_reference);

-- 2. Table: booking_seats
CREATE TABLE booking_seats (
    id BINARY(16) NOT NULL PRIMARY KEY,
    booking_id BINARY(16) NOT NULL,
    show_seat_id BINARY(16) NOT NULL,   -- Logical FK to Show Service (show_seats.id)
    seat_number VARCHAR(20) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT uk_booking_seats_booking_showseat UNIQUE (booking_id, show_seat_id),
    CONSTRAINT chk_booking_seats_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);
