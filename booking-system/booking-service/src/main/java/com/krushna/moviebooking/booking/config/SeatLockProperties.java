package com.krushna.moviebooking.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Redis Seat Locking.
 */
@Data
@Component
@ConfigurationProperties(prefix = "booking.seat-lock")
public class SeatLockProperties {

    /**
     * Default lock expiration TTL in seconds. Default is 300 seconds (5 minutes).
     */
    private long defaultTtlSeconds = 300;

    /**
     * Prefix for Redis lock keys. Default is "seat:".
     */
    private String keyPrefix = "seat:";

    /**
     * Scheduled monitoring interval in milliseconds.
     */
    private long schedulerIntervalMs = 60000;
}
