package com.krushna.moviebooking.booking.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain Event published to Kafka when a booking status changes to CONFIRMED.
 */
@Builder
public record BookingConfirmedEvent(
        String eventId,
        String eventType,
        Integer eventVersion,
        UUID bookingId,
        String bookingReference,
        UUID userId,
        UUID showId,
        List<UUID> showSeatIds,
        List<String> seatNumbers,
        BigDecimal totalAmount,
        String paymentId,
        Instant timestamp
) {
    public static class BookingConfirmedEventBuilder {
        private String eventId = UUID.randomUUID().toString();
        private String eventType = "BOOKING_CONFIRMED";
        private Integer eventVersion = 1;
        private Instant timestamp = Instant.now();
    }
}
