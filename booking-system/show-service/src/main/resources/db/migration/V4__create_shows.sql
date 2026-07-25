-- Flyway Migration V4: Show Service (Shows and ShowSeats Aggregate)
-- Target Database: show_db

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Function to handle automatic updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. Table: shows
CREATE TABLE shows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,        -- Logical FK to Movie Service
    theatre_id UUID NOT NULL,      -- Logical FK to Theatre Service
    screen_id UUID NOT NULL,       -- Logical FK to Theatre Service
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    language VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shows_time CHECK (end_time > start_time),
    CONSTRAINT chk_shows_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED'))
);

CREATE TRIGGER trg_shows_updated_at
BEFORE UPDATE ON shows
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Indexes on shows table
CREATE INDEX idx_shows_movie_time ON shows(movie_id, start_time) WHERE deleted_at IS NULL;
CREATE INDEX idx_shows_theatre_time ON shows(theatre_id, start_time) WHERE deleted_at IS NULL;
CREATE INDEX idx_shows_screen_time ON shows(screen_id, start_time, end_time) WHERE deleted_at IS NULL;

-- 2. Table: show_seats
CREATE TABLE show_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL,
    seat_id UUID NOT NULL,        -- Logical FK to Theatre Service (seats.id)
    price NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    lock_expiration TIMESTAMPTZ NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_show_seats_show FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE,
    CONSTRAINT uk_show_seats_show_seat UNIQUE (show_id, seat_id),
    CONSTRAINT chk_show_seats_price CHECK (price >= 0),
    CONSTRAINT chk_show_seats_status CHECK (status IN ('AVAILABLE', 'BLOCKED', 'BOOKED', 'OUT_OF_ORDER'))
);

CREATE TRIGGER trg_show_seats_updated_at
BEFORE UPDATE ON show_seats
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Indexes on show_seats table
CREATE INDEX idx_show_seats_show_status ON show_seats(show_id, status);
CREATE INDEX idx_show_seats_status_lock ON show_seats(status, lock_expiration) WHERE status = 'BLOCKED';
