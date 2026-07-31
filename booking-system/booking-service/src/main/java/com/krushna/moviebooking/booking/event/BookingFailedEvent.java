package com.krushna.moviebooking.booking.event;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Event published to Kafka when booking creation or payment fails.
 */
@Builder
public record BookingFailedEvent(
        String eventId,
        String eventType,
        Integer eventVersion,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        UUID showId,
        List<UUID> showSeatIds,
        String failureReason,
        Instant timestamp
) {
    public static class BookingFailedEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = "BOOKING_FAILED";
        private Integer eventVersion = 1;
        private Instant timestamp = Instant.now();
    }
}
