package com.krushna.moviebooking.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class BookingEvents {
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
    ) {}

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
    ) {}
}
