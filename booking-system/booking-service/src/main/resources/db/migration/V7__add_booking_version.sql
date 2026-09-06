-- Flyway Migration V7: Add version column to bookings table for JPA optimistic locking
-- Target Database: vibecheck_booking (MySQL 8.0)

ALTER TABLE bookings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
