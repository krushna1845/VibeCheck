CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    booking_id UUID NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    user_id UUID,
    movie_title VARCHAR(255) NOT NULL,
    theatre_name VARCHAR(255) NOT NULL,
    screen_name VARCHAR(100) NOT NULL,
    seat_numbers VARCHAR(255) NOT NULL,
    show_date DATE,
    show_time TIME,
    amount DECIMAL(10, 2) NOT NULL,
    qr_code_data TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_booking_ref ON tickets(booking_reference);
