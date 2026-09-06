-- Flyway Migration V2: Show Service (Shows and ShowSeats Aggregate)
-- Target Database: vibecheck_show (MySQL 8.0)

-- 1. Table: shows
CREATE TABLE shows (
    id BINARY(16) NOT NULL PRIMARY KEY,
    movie_id BINARY(16) NOT NULL,        -- Logical FK to Movie Service
    theatre_id BINARY(16) NOT NULL,      -- Logical FK to Theatre Service
    screen_id BINARY(16) NOT NULL,       -- Logical FK to Theatre Service
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    language VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_shows_time CHECK (end_time > start_time),
    CONSTRAINT chk_shows_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes on shows table
CREATE INDEX idx_shows_movie_time ON shows(movie_id, start_time);
CREATE INDEX idx_shows_theatre_time ON shows(theatre_id, start_time);
CREATE INDEX idx_shows_screen_time ON shows(screen_id, start_time, end_time);

-- 2. Table: show_seats
CREATE TABLE show_seats (
    id BINARY(16) NOT NULL PRIMARY KEY,
    show_id BINARY(16) NOT NULL,
    seat_id BINARY(16) NOT NULL,        -- Logical FK to Theatre Service (seats.id)
    price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    lock_expiration DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_show_seats_show FOREIGN KEY (show_id) REFERENCES shows(id) ON DELETE CASCADE,
    CONSTRAINT uk_show_seats_show_seat UNIQUE (show_id, seat_id),
    CONSTRAINT chk_show_seats_price CHECK (price >= 0),
    CONSTRAINT chk_show_seats_status CHECK (status IN ('AVAILABLE', 'BLOCKED', 'BOOKED', 'OUT_OF_ORDER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes on show_seats table
CREATE INDEX idx_show_seats_show_status ON show_seats(show_id, status);
CREATE INDEX idx_show_seats_status_lock ON show_seats(status, lock_expiration);
