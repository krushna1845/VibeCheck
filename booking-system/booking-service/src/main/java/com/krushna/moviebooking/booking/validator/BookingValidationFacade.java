package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.entity.Booking;

import java.util.UUID;

/**
 * Facade interface coordinating all domain validators (UserValidator, ShowValidator,
 * SeatValidator, BookingValidator) for unified workflow validations.
 */
public interface BookingValidationFacade {

    /**
     * Validates an incoming booking creation request end-to-end:
     * payload structure, user existence/active/roles, show existence/active/time,
     * and seat existence/show alignment/active/availability.
     *
     * @param request Booking creation request payload
     */
    void validateBookingCreation(BookingRequest request);

    /**
     * Validates that a user owns the target booking.
     *
     * @param booking Target booking entity
     * @param userId Requesting user ID
     */
    void validateBookingOwnership(Booking booking, UUID userId);

    /**
     * Validates a state transition for the given booking.
     *
     * @param booking Target booking entity
     * @param targetStatus Target state string
     */
    void validateBookingStateTransition(Booking booking, String targetStatus);

    /**
     * Validates cancellation eligibility (ownership + state transition).
     *
     * @param booking Target booking entity
     * @param userId Requesting user ID
     */
    void validateBookingCancellation(Booking booking, UUID userId);

    /**
     * Validates confirmation eligibility (state transition).
     *
     * @param booking Target booking entity
     * @param paymentId Payment transaction reference
     */
    void validateBookingConfirmation(Booking booking, String paymentId);
}
