package com.krushna.moviebooking.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight outbound DTO used for listing and page responses.
 *
 * @param id               Booking primary UUID
 * @param bookingReference Unique 12-character alphanumeric reference code
 * @param userId           Customer user reference ID
 * @param showId           Show reference ID
 * @param totalAmount      Total monetary value charged
 * @param status           Current lifecycle status
 * @param expiresAt        Expiration timestamp
 * @param createdAt        Creation timestamp
 * @param seatCount        Total seats reserved in this booking
 */
@Builder
public record BookingSummary(
        UUID id,
        String bookingReference,
        UUID userId,
        UUID showId,
        BigDecimal totalAmount,
        String status,
        Instant expiresAt,
        Instant createdAt,
        int seatCount
) {}
