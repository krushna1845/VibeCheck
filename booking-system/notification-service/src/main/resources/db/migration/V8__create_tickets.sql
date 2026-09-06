-- Flyway Migration V8: Create tickets table
-- Target Database: vibecheck_notification (MySQL 8.0)

CREATE TABLE IF NOT EXISTS tickets (
    id BINARY(16) NOT NULL PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    booking_id BINARY(16) NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    user_id BINARY(16) NULL,
    movie_title VARCHAR(255) NOT NULL,
    theatre_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    seat_numbers VARCHAR(255) NOT NULL,
    show_date DATE NULL,
    show_time TIME NULL,
    amount DECIMAL(10, 2) NOT NULL,
    qr_code_data TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_booking_ref ON tickets(booking_reference);
