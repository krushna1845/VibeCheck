package com.krushna.moviebooking.booking.event;

/**
 * Enumeration of seat availability update types emitted over WebSocket.
 */
public enum SeatAvailabilityEventType {

    /**
     * Seats locked temporarily by a user during booking creation.
     */
    SEAT_LOCKED,

    /**
     * Seats released back to AVAILABLE status (lock expired, booking cancelled, or manual unlock).
     */
    SEAT_RELEASED,

    /**
     * Seats permanently reserved following successful payment confirmation.
     */
    BOOKING_CONFIRMED,

    /**
     * Booking cancelled by user or admin, restoring seats to available.
     */
    BOOKING_CANCELLED,

    /**
     * Booking reservation TTL expired without payment.
     */
    BOOKING_EXPIRED
}
