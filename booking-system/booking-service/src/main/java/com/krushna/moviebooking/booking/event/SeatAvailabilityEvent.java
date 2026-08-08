package com.krushna.moviebooking.booking.event;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event payload broadcasted over STOMP WebSocket to topic subscribers
 * at {@code /topic/show/{showId}} when real-time seat availability changes.
 */
@Builder
public record SeatAvailabilityEvent(
        String eventId,
        SeatAvailabilityEventType eventType,
        UUID showId,
        List<UUID> showSeatIds,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        Instant timestamp
) {
    public static class SeatAvailabilityEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private Instant timestamp = Instant.now();
    }
}
