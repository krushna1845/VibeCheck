package com.krushna.moviebooking.booking.validator.impl;

import com.krushna.moviebooking.booking.client.ShowClient;
import com.krushna.moviebooking.booking.client.UserClient;
import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.exception.InvalidBookingRequestException;
import com.krushna.moviebooking.booking.validator.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary implementation of {@link BookingValidationFacade}.
 * Coordinates individual domain validators in a structured, sequential workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingValidationFacadeImpl implements BookingValidationFacade {

    private final BookingValidator bookingValidator;
    private final SeatValidator seatValidator;
    private final ShowValidator showValidator;
    private final UserValidator userValidator;
    private final ShowClient showClient;
    private final UserClient userClient;

    @Override
    public void validateBookingCreation(BookingRequest request) {
        log.info("Executing facade validation for booking creation request: userId={}, showId={}",
                request != null ? request.userId() : null, request != null ? request.showId() : null);

        // 1. Structure validation
        bookingValidator.validateBookingRequest(request);

        // 2. User validation
        Optional<UserClient.UserDto> userOpt = userClient.getUserById(request.userId());
        userValidator.validateUser(userOpt, request.userId());

        // 3. Show validation
        Optional<ShowClient.ShowDto> showOpt = showClient.getShowById(request.showId());
        showValidator.validateShow(showOpt, request.showId());

        // 4. Seat validation
        List<ShowClient.ShowSeatDto> seats = showClient.getShowSeatsByIds(request.showId(), request.showSeatIds());
        seatValidator.validateSeats(request.showId(), request.showSeatIds(), seats);

        log.info("Booking creation validation passed successfully for userId={}, showId={}",
                request.userId(), request.showId());
    }

    @Override
    public void validateBookingOwnership(Booking booking, UUID userId) {
        bookingValidator.validateBookingOwnership(booking, userId);
    }

    @Override
    public void validateBookingStateTransition(Booking booking, String targetStatus) {
        bookingValidator.validateBookingState(booking, targetStatus);
    }

    @Override
    public void validateBookingCancellation(Booking booking, UUID userId) {
        log.info("Executing facade validation for booking cancellation: bookingId={}, userId={}",
                booking != null ? booking.getId() : null, userId);

        if (userId != null) {
            bookingValidator.validateBookingOwnership(booking, userId);
        }
        bookingValidator.validateBookingState(booking, "CANCELLED");
    }

    @Override
    public void validateBookingConfirmation(Booking booking, String paymentId) {
        log.info("Executing facade validation for booking confirmation: bookingId={}, paymentId={}",
                booking != null ? booking.getId() : null, paymentId);

        if (paymentId == null || paymentId.isBlank()) {
            throw new InvalidBookingRequestException("Payment reference ID must not be null or blank");
        }
        bookingValidator.validateBookingState(booking, "CONFIRMED");
    }
}
