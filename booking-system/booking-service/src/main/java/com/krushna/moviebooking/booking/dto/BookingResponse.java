package com.krushna.moviebooking.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed outbound DTO returned from Booking Service operations.
 *
 * @param id               Booking primary UUID
 * @param bookingReference Unique 12-character alphanumeric reference code
 * @param userId           Customer user reference ID
 * @param showId           Show reference ID
 * @param totalAmount      Total monetary value charged
 * @param taxAmount        Tax amount included
 * @param convenienceFee  Convenience fee included
 * @param status           Current lifecycle status (PENDING, CONFIRMED, CANCELLED, EXPIRED)
 * @param expiresAt        Expiration timestamp for pending reservation
 * @param createdAt        Creation timestamp
 * @param updatedAt        Last modified timestamp
 * @param seats            Detailed list of booked seats
 */
@Builder
public record BookingResponse(
        UUID id,
        String bookingReference,
        UUID userId,
        UUID showId,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        BigDecimal convenienceFee,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt,
        List<BookingSeatResponse> seats
) {}
