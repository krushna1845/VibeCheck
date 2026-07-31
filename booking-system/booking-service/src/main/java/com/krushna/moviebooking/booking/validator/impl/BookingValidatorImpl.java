package com.krushna.moviebooking.booking.validator.impl;

import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.exception.InvalidBookingOwnershipException;
import com.krushna.moviebooking.booking.exception.InvalidBookingRequestException;
import com.krushna.moviebooking.booking.exception.InvalidBookingStateException;
import com.krushna.moviebooking.booking.validator.BookingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Primary implementation of {@link BookingValidator}.
 */
@Slf4j
@Component
public class BookingValidatorImpl implements BookingValidator {

    /**
     * Allowed state transitions per state machine specification.
     */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "CREATED", Set.of("SEATS_LOCKED", "CANCELLED", "EXPIRED", "PENDING"),
            "SEATS_LOCKED", Set.of("PAYMENT_PENDING", "PENDING", "FAILED", "EXPIRED", "CANCELLED"),
            "PENDING", Set.of("CONFIRMED", "FAILED", "EXPIRED", "CANCELLED"),
            "PAYMENT_PENDING", Set.of("CONFIRMED", "FAILED", "EXPIRED", "CANCELLED"),
            "CONFIRMED", Set.of("COMPLETED", "CANCELLED")
    );

    @Override
    public void validateBookingRequest(BookingRequest request) {
        log.debug("Validating booking request payload...");
        if (request == null) {
            throw new InvalidBookingRequestException("Booking request body must not be null");
        }
        if (request.userId() == null) {
            throw new InvalidBookingRequestException("User reference ID must not be null");
        }
        if (request.showId() == null) {
            throw new InvalidBookingRequestException("Show reference ID must not be null");
        }
        if (request.showSeatIds() == null || request.showSeatIds().isEmpty()) {
            throw new InvalidBookingRequestException("At least one seat must be selected for booking");
        }
        if (request.showSeatIds().size() > MAX_SEATS_PER_BOOKING) {
            throw new InvalidBookingRequestException(
                    String.format("Cannot book more than %d seats in a single request", MAX_SEATS_PER_BOOKING));
        }
    }

    @Override
    public void validateBookingOwnership(Booking booking, UUID userId) {
        log.debug("Validating booking ownership for booking ID: {}, userId: {}",
                booking != null ? booking.getId() : null, userId);

        if (booking == null) {
            throw new InvalidBookingRequestException("Booking entity must not be null");
        }
        if (userId == null) {
            throw new InvalidBookingOwnershipException("Requesting user ID must not be null");
        }
        if (!userId.equals(booking.getUserId())) {
            log.warn("Ownership violation: Booking {} owned by user {}, requested by user {}",
                    booking.getId(), booking.getUserId(), userId);
            throw new InvalidBookingOwnershipException(booking.getId(), userId);
        }
    }

    @Override
    public void validateBookingState(Booking booking, String targetStatus) {
        log.debug("Validating booking state transition from {} to {}",
                booking != null ? booking.getStatus() : null, targetStatus);

        if (booking == null) {
            throw new InvalidBookingRequestException("Booking entity must not be null");
        }
        if (targetStatus == null || targetStatus.isBlank()) {
            throw new InvalidBookingStateException("Target status must not be null or blank");
        }

        String currentStatus = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "";
        String normalizedTarget = targetStatus.trim().toUpperCase();

        if (currentStatus.equals(normalizedTarget)) {
            log.debug("Booking {} already in status {}. Idempotent validation.", booking.getId(), normalizedTarget);
            return;
        }

        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(normalizedTarget)) {
            log.warn("Forbidden state transition for booking {}: {} -> {}", booking.getId(), currentStatus, normalizedTarget);
            throw new InvalidBookingStateException(
                    String.format("Illegal booking state transition from %s to %s for booking %s",
                            currentStatus, normalizedTarget, booking.getBookingReference() != null ? booking.getBookingReference() : booking.getId()));
        }
    }
}
