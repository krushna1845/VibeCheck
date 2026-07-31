package com.krushna.moviebooking.booking.validator.impl;

import com.krushna.moviebooking.booking.client.ShowClient.ShowSeatDto;
import com.krushna.moviebooking.booking.exception.SeatInactiveException;
import com.krushna.moviebooking.booking.exception.SeatNotAvailableException;
import com.krushna.moviebooking.booking.exception.SeatNotFoundException;
import com.krushna.moviebooking.booking.validator.SeatValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Primary implementation of {@link SeatValidator}.
 */
@Slf4j
@Component
public class SeatValidatorImpl implements SeatValidator {

    private static final Set<String> INACTIVE_STATUSES = Set.of("BLOCKED", "INACTIVE", "DISABLED", "MAINTENANCE");
    private static final String AVAILABLE_STATUS = "AVAILABLE";

    @Override
    public void validateSeatsExist(UUID showId, List<UUID> requestedSeatIds, List<ShowSeatDto> fetchedSeats) {
        log.debug("Validating seat existence for showId: {}", showId);
        if (fetchedSeats == null || fetchedSeats.size() != requestedSeatIds.size()) {
            Set<UUID> foundIds = fetchedSeats != null ?
                    fetchedSeats.stream().map(ShowSeatDto::id).collect(java.util.stream.Collectors.toSet()) : Set.of();

            List<UUID> missingIds = requestedSeatIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            log.warn("Missing seats detected for showId {}: {}", showId, missingIds);
            throw new SeatNotFoundException(showId, missingIds);
        }
    }

    @Override
    public void validateSeatsBelongToShow(UUID showId, List<ShowSeatDto> fetchedSeats) {
        log.debug("Validating seats belong to showId: {}", showId);
        List<UUID> mismatchSeats = fetchedSeats.stream()
                .filter(seat -> !showId.equals(seat.showId()))
                .map(ShowSeatDto::id)
                .toList();

        if (!mismatchSeats.isEmpty()) {
            log.warn("Seats do not belong to showId {}: {}", showId, mismatchSeats);
            throw new SeatNotFoundException(showId, mismatchSeats);
        }
    }

    @Override
    public void validateSeatsActive(UUID showId, List<ShowSeatDto> fetchedSeats) {
        log.debug("Validating seats active state for showId: {}", showId);
        List<UUID> inactiveSeats = fetchedSeats.stream()
                .filter(seat -> seat.status() != null && INACTIVE_STATUSES.contains(seat.status().toUpperCase()))
                .map(ShowSeatDto::id)
                .toList();

        if (!inactiveSeats.isEmpty()) {
            log.warn("Inactive seats detected for showId {}: {}", showId, inactiveSeats);
            throw new SeatInactiveException(showId, inactiveSeats);
        }
    }

    @Override
    public void validateSeatsAvailable(UUID showId, List<ShowSeatDto> fetchedSeats) {
        log.debug("Validating seats availability for showId: {}", showId);
        List<UUID> unavailableSeats = fetchedSeats.stream()
                .filter(seat -> seat.status() == null || !AVAILABLE_STATUS.equalsIgnoreCase(seat.status()))
                .map(ShowSeatDto::id)
                .toList();

        if (!unavailableSeats.isEmpty()) {
            log.warn("Unavailable seats detected for showId {}: {}", showId, unavailableSeats);
            throw new SeatNotAvailableException(showId, unavailableSeats);
        }
    }

    @Override
    public void validateSeats(UUID showId, List<UUID> requestedSeatIds, List<ShowSeatDto> fetchedSeats) {
        validateSeatsExist(showId, requestedSeatIds, fetchedSeats);
        validateSeatsBelongToShow(showId, fetchedSeats);
        validateSeatsActive(showId, fetchedSeats);
        validateSeatsAvailable(showId, fetchedSeats);
    }
}
