package com.krushna.moviebooking.booking.event;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Event published to Kafka when a pending booking reservation expires.
 */
@Builder
public record BookingExpiredEvent(
        String eventId,
        String eventType,
        Integer eventVersion,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        UUID showId,
        List<UUID> showSeatIds,
        Instant timestamp
) {
    public static class BookingExpiredEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = "BOOKING_EXPIRED";
        private Integer eventVersion = 1;
        private Instant timestamp = Instant.now();
    }
}
