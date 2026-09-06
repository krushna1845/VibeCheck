-- Flyway Migration V1: Booking Service scaffold initialization
-- Target Database: vibecheck_booking (MySQL 8.0)
CREATE TABLE IF NOT EXISTS booking_service_domain (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    created_at  DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
