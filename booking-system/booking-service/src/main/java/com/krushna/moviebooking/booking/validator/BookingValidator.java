package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.entity.Booking;

import java.util.UUID;

/**
 * Validator interface for booking request structure, ownership, and state machine transition rules.
 */
public interface BookingValidator {

    /**
     * Maximum allowed seats per booking operation.
     */
    int MAX_SEATS_PER_BOOKING = 10;

    /**
     * Validates that the booking request payload satisfies basic constraints:
     * non-null attributes, non-empty seats, and maximum seat limits.
     *
     * @param request Booking creation request payload
     */
    void validateBookingRequest(BookingRequest request);

    /**
     * Validates that the specified user owns the given booking.
     *
     * @param booking Target booking entity
     * @param userId ID of the requesting user
     */
    void validateBookingOwnership(Booking booking, UUID userId);

    /**
     * Validates that transitioning the booking to a target status is legal according
     * to the booking state machine matrix.
     *
     * @param booking Target booking entity
     * @param targetStatus Target state string
     */
    void validateBookingState(Booking booking, String targetStatus);
}
