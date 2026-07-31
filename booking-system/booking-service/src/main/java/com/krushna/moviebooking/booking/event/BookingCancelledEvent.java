package com.krushna.moviebooking.booking.event;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Event published to Kafka when a booking is cancelled.
 */
@Builder
public record BookingCancelledEvent(
        String eventId,
        String eventType,
        Integer eventVersion,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        UUID showId,
        List<UUID> showSeatIds,
        String reason,
        Instant timestamp
) {
    public static class BookingCancelledEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = "BOOKING_CANCELLED";
        private Integer eventVersion = 1;
        private Instant timestamp = Instant.now();
    }
}
