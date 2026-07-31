package com.krushna.moviebooking.booking.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Event published to Kafka when a new booking is created in PENDING status.
 */
@Builder
public record BookingCreatedEvent(
        String eventId,
        String eventType,
        Integer eventVersion,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        UUID showId,
        List<UUID> showSeatIds,
        BigDecimal totalAmount,
        Instant expiresAt,
        Instant timestamp
) {
    public static class BookingCreatedEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = "BOOKING_CREATED";
        private Integer eventVersion = 1;
        private Instant timestamp = Instant.now();
    }
}
