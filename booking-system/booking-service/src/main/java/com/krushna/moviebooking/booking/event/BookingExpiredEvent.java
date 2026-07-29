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
        UUID bookingId,
        String bookingReference,
        UUID userId,
        UUID showId,
        List<UUID> showSeatIds,
        Instant timestamp
) {}
