-- Flyway Migration V3: Theatre Service (Cities, Theatres, Screens, Seats Aggregate)
-- Target Database: vibecheck_theatre (MySQL 8.0)

-- 1. Table: theatres
CREATE TABLE theatres (
    id BINARY(16) NOT NULL PRIMARY KEY,
    city_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_theatres_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE RESTRICT,
    CONSTRAINT chk_theatres_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'RENOVATION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_theatres_city_id ON theatres(city_id);
CREATE INDEX idx_theatres_status ON theatres(status);

-- 2. Table: screens
CREATE TABLE screens (
    id BINARY(16) NOT NULL PRIMARY KEY,
    theatre_id BINARY(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    screen_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    total_seats INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_screens_theatre FOREIGN KEY (theatre_id) REFERENCES theatres(id) ON DELETE CASCADE,
    CONSTRAINT uk_screens_theatre_name UNIQUE (theatre_id, name),
    CONSTRAINT chk_screens_total_seats CHECK (total_seats > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_screens_theatre_id ON screens(theatre_id);

-- 3. Table: seats
CREATE TABLE seats (
    id BINARY(16) NOT NULL PRIMARY KEY,
    screen_id BINARY(16) NOT NULL,
    seat_row VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    seat_category VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_seats_screen FOREIGN KEY (screen_id) REFERENCES screens(id) ON DELETE CASCADE,
    CONSTRAINT uk_seats_screen_row_num UNIQUE (screen_id, seat_row, seat_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_seats_screen_id ON seats(screen_id);
CREATE INDEX idx_seats_screen_category ON seats(screen_id, seat_category);
