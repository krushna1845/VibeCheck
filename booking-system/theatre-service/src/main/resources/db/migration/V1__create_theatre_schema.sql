-- Flyway Migration V1: Theatre Service Schema
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE cities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL
);

CREATE TABLE theatres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id INT NOT NULL REFERENCES cities(id),
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    latitude NUMERIC(10, 8),
    longitude NUMERIC(11, 8),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_theatres_city_name UNIQUE (city_id, name)
);

CREATE INDEX idx_theatres_city_id ON theatres(city_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_theatres_name ON theatres(name) WHERE deleted_at IS NULL;

CREATE TABLE screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theatre_id UUID NOT NULL REFERENCES theatres(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    screen_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    total_seats INT NOT NULL CHECK (total_seats >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_screens_theatre_name UNIQUE (theatre_id, name)
);

CREATE INDEX idx_screens_theatre_id ON screens(theatre_id);

CREATE TABLE seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id UUID NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    seat_row VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    seat_category VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_seats_screen_row_num UNIQUE (screen_id, seat_row, seat_number)
);

CREATE INDEX idx_seats_screen_id ON seats(screen_id);
