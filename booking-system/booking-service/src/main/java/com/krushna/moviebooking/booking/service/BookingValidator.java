package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.dto.BookingRequest;

/**
 * Service interface performing pre-booking business validations.
 */
public interface BookingValidator {

    /**
     * Validates that the booking request conforms to all business rules:
     * show existence, seat availability, active state, and non-empty selection.
     *
     * @param request Booking creation request payload
     */
    void validateBookingRequest(BookingRequest request);
}
