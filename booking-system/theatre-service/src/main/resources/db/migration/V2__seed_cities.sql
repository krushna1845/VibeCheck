-- Flyway Migration V2: Create Cities Table and Seed Data
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE cities (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    pincode VARCHAR(20) NULL,
    CONSTRAINT uk_cities_name_state UNIQUE (name, state)
);

CREATE INDEX idx_cities_name ON cities(name);

INSERT INTO cities (name, state, pincode) VALUES
('Mumbai', 'Maharashtra', '400001'),
('Delhi', 'Delhi', '110001'),
('Bengaluru', 'Karnataka', '560001'),
('Hyderabad', 'Telangana', '500001'),
('Chennai', 'Tamil Nadu', '600001'),
('Kolkata', 'West Bengal', '700001'),
('Pune', 'Maharashtra', '411001'),
('Ahmedabad', 'Gujarat', '380001')
ON CONFLICT (name, state) DO NOTHING;
