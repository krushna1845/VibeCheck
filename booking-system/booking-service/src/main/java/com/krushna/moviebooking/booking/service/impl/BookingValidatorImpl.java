package com.krushna.moviebooking.booking.service.impl;

import com.krushna.moviebooking.booking.client.ShowClient;
import com.krushna.moviebooking.booking.dto.BookingRequest;
import com.krushna.moviebooking.booking.exception.InvalidBookingRequestException;
import com.krushna.moviebooking.booking.exception.SeatNotAvailableException;
import com.krushna.moviebooking.booking.service.BookingValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Primary implementation of {@link BookingValidator}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingValidatorImpl implements BookingValidator {

    private final ShowClient showClient;

    @Override
    public void validateBookingRequest(BookingRequest request) {
        log.info("Validating booking request for userId: {}, showId: {}, seatCount: {}",
                request.userId(), request.showId(), request.showSeatIds() != null ? request.showSeatIds().size() : 0);

        if (request.userId() == null) {
            throw new InvalidBookingRequestException("User reference ID must not be null");
        }
        if (request.showId() == null) {
            throw new InvalidBookingRequestException("Show reference ID must not be null");
        }
        if (request.showSeatIds() == null || request.showSeatIds().isEmpty()) {
            throw new InvalidBookingRequestException("At least one seat must be selected for booking");
        }

        if (!showClient.existsShow(request.showId())) {
            throw new InvalidBookingRequestException("Requested show with ID " + request.showId() + " does not exist");
        }

        List<ShowClient.ShowSeatDto> seats = showClient.getShowSeatsByIds(request.showId(), request.showSeatIds());
        if (seats.size() != request.showSeatIds().size()) {
            throw new InvalidBookingRequestException("One or more selected seats do not exist for the show");
        }

        List<UUID> unavailableSeats = seats.stream()
                .filter(seat -> !"AVAILABLE".equalsIgnoreCase(seat.status()))
                .map(ShowClient.ShowSeatDto::id)
                .toList();

        if (!unavailableSeats.isEmpty()) {
            log.warn("Unavailable seats detected for showId {}: {}", request.showId(), unavailableSeats);
            throw new SeatNotAvailableException(request.showId(), unavailableSeats);
        }
    }
}
