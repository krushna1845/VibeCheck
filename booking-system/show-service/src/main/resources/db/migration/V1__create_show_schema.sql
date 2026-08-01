-- Flyway Migration V1: Show Service Schema
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE shows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    theatre_id UUID NOT NULL,
    screen_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    language VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shows_time CHECK (end_time > start_time)
);

CREATE INDEX idx_shows_movie_id ON shows(movie_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_shows_screen_id ON shows(screen_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_shows_theatre_id ON shows(theatre_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_shows_start_time ON shows(start_time) WHERE deleted_at IS NULL;

CREATE TABLE show_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_id UUID NOT NULL,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_show_seats_show_seat UNIQUE (show_id, seat_id)
);

CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
