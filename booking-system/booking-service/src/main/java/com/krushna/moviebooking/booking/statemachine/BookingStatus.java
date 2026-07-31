package com.krushna.moviebooking.booking.statemachine;

import java.util.Arrays;
import java.util.Optional;

/**
 * Canonical enumeration of every state a {@link com.krushna.moviebooking.booking.entity.Booking}
 * can occupy during its lifecycle.
 *
 * <p>Terminal states ({@link #COMPLETED}, {@link #FAILED}, {@link #CANCELLED}, {@link #EXPIRED})
 * have no outgoing transitions and cannot accept further state changes.
 *
 * <p>Lifecycle diagram (abbreviated):
 * <pre>
 *   CREATED ──► SEATS_LOCKED ──► PAYMENT_PENDING ──► CONFIRMED ──► COMPLETED
 *      │              │                  │
 *      └─► CANCELLED  └─► FAILED         └─► FAILED
 *      └─► EXPIRED    └─► EXPIRED        └─► EXPIRED
 * </pre>
 */
public enum BookingStatus {

    /**
     * Booking record has been created but seats are not yet locked.
     * Redis lock not yet confirmed; customer has not started payment.
     */
    CREATED(false),

    /**
     * Requested seats have been successfully locked in Redis.
     * Seats are unavailable to other customers; lock has an expiration TTL.
     */
    SEATS_LOCKED(false),

    /**
     * Customer is currently completing payment.
     * Waiting for payment gateway callback; Redis locks remain active.
     */
    PAYMENT_PENDING(false),

    /**
     * Payment has been successfully verified.
     * Booking is permanent; seats are permanently reserved.
     */
    CONFIRMED(false),

    /**
     * Entire booking workflow has finished successfully.
     * Notification sent; audit log completed.
     * <strong>Terminal state – no further transitions allowed.</strong>
     */
    COMPLETED(true),

    /**
     * Booking failed due to business or payment failure.
     * Redis locks released; seats become available again.
     * <strong>Terminal state – no further transitions allowed.</strong>
     */
    FAILED(true),

    /**
     * Booking cancelled before confirmation by customer or administrator.
     * Seats released; booking closed.
     * <strong>Terminal state – no further transitions allowed.</strong>
     */
    CANCELLED(true),

    /**
     * Customer failed to complete booking within the lock timeout.
     * Scheduler releases Redis locks; booking closed automatically.
     * <strong>Terminal state – no further transitions allowed.</strong>
     */
    EXPIRED(true);

    // -----------------------------------------------------------------------
    // State properties
    // -----------------------------------------------------------------------

    /** Whether this status represents a terminal state (no further transitions). */
    private final boolean terminal;

    BookingStatus(boolean terminal) {
        this.terminal = terminal;
    }

    /**
     * Returns {@code true} if this state is terminal and accepts no further transitions.
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Returns {@code true} if this state is non-terminal (transitions still possible).
     */
    public boolean isActive() {
        return !terminal;
    }

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    /**
     * Parses a status string (case-insensitive) into a {@link BookingStatus}.
     *
     * @param value raw status string, e.g. {@code "CONFIRMED"} or {@code "confirmed"}
     * @return corresponding {@link BookingStatus}
     * @throws BookingTransitionException if the value does not map to any known status
     */
    public static BookingStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new BookingTransitionException(
                    "BookingStatus value must not be null or blank");
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new BookingTransitionException(
                        "Unknown BookingStatus: '" + value + "'"));
    }

    /**
     * Safe lookup that returns an {@link Optional} instead of throwing.
     *
     * @param value raw status string
     * @return {@link Optional} containing the matched status, or empty if unrecognised
     */
    public static Optional<BookingStatus> fromSafe(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
