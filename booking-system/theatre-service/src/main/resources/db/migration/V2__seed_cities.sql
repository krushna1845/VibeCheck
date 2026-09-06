-- Flyway Migration V2: Create Cities Table and Seed Data
-- Target Database: vibecheck_theatre (MySQL 8.0)

CREATE TABLE cities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    pincode VARCHAR(20) NULL,
    CONSTRAINT uk_cities_name_state UNIQUE (name, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cities_name ON cities(name);

INSERT IGNORE INTO cities (name, state, pincode) VALUES
('Mumbai', 'Maharashtra', '400001'),
('Delhi', 'Delhi', '110001'),
('Bengaluru', 'Karnataka', '560001'),
('Hyderabad', 'Telangana', '500001'),
('Chennai', 'Tamil Nadu', '600001'),
('Kolkata', 'West Bengal', '700001'),
('Pune', 'Maharashtra', '411001'),
('Ahmedabad', 'Gujarat', '380001');
