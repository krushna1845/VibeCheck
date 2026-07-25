-- Flyway Migration V3: Theatre Service (Cities, Theatres, Screens, Seats Aggregate)
-- Target Database: theatre_db

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Function to handle automatic updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1. Table: cities
CREATE TABLE cities (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    pincode VARCHAR(20) NULL,
    CONSTRAINT uk_cities_name_state UNIQUE (name, state)
);

CREATE INDEX idx_cities_name ON cities(name);

-- 2. Table: theatres
CREATE TABLE theatres (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    city_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    latitude NUMERIC(10, 8) NULL,
    longitude NUMERIC(11, 8) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_theatres_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE RESTRICT,
    CONSTRAINT chk_theatres_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'RENOVATION'))
);

CREATE TRIGGER trg_theatres_updated_at
BEFORE UPDATE ON theatres
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_theatres_city_id ON theatres(city_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_theatres_status ON theatres(status);

-- 3. Table: screens
CREATE TABLE screens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    theatre_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    screen_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    total_seats INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_screens_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(id) ON DELETE CASCADE,
    CONSTRAINT uk_screens_theatre_name UNIQUE (theatre_id, name),
    CONSTRAINT chk_screens_total_seats CHECK (total_seats > 0)
);

CREATE INDEX idx_screens_theatre_id ON screens(theatre_id);

-- 4. Table: seats
CREATE TABLE seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    screen_id UUID NOT NULL,
    seat_row VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    seat_category VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_seats_screen FOREIGN KEY (screen_id) REFERENCES screens(id) ON DELETE CASCADE,
    CONSTRAINT uk_seats_screen_row_num UNIQUE (screen_id, seat_row, seat_number)
);

CREATE INDEX idx_seats_screen_id ON seats(screen_id);
CREATE INDEX idx_seats_screen_category ON seats(screen_id, seat_category);
