package com.krushna.moviebooking.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin booking search filter criteria passed as query parameters.
 *
 * <p>All fields are optional. When present, they are combined with logical AND.
 *
 * @param status   Filter by booking status (PENDING, CONFIRMED, CANCELLED, EXPIRED)
 * @param userId   Filter by customer user UUID
 * @param showId   Filter by show UUID
 * @param dateFrom Lower bound for {@code createdAt} (inclusive, ISO-8601)
 * @param dateTo   Upper bound for {@code createdAt} (inclusive, ISO-8601)
 */
@Schema(description = "Admin booking search filter criteria")
public record BookingSearchCriteria(

        @Schema(description = "Booking status filter", example = "CONFIRMED",
                allowableValues = {"PENDING", "CONFIRMED", "CANCELLED", "EXPIRED"})
        String status,

        @Schema(description = "Customer user ID filter", example = "a1b2c3d4-0000-0000-0000-000000000001")
        UUID userId,

        @Schema(description = "Show ID filter", example = "b2c3d4e5-0000-0000-0000-000000000002")
        UUID showId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Schema(description = "Created-at lower bound (ISO-8601 UTC)", example = "2026-01-01T00:00:00Z")
        Instant dateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Schema(description = "Created-at upper bound (ISO-8601 UTC)", example = "2026-12-31T23:59:59Z")
        Instant dateTo

) {}
