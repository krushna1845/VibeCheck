package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.client.ShowClient.ShowSeatDto;

import java.util.List;
import java.util.UUID;

/**
 * Validator interface for seat existence, show association, active state, and availability rules.
 */
public interface SeatValidator {

    /**
     * Validates that all requested seat IDs exist in the show catalog.
     *
     * @param showId Target show ID
     * @param requestedSeatIds List of requested seat IDs
     * @param fetchedSeats List of fetched show seat DTOs
     */
    void validateSeatsExist(UUID showId, List<UUID> requestedSeatIds, List<ShowSeatDto> fetchedSeats);

    /**
     * Validates that all fetched seats belong to the specified show.
     *
     * @param showId Target show ID
     * @param fetchedSeats List of fetched show seat DTOs
     */
    void validateSeatsBelongToShow(UUID showId, List<ShowSeatDto> fetchedSeats);

    /**
     * Validates that all fetched seats are active and not disabled or blocked.
     *
     * @param showId Target show ID
     * @param fetchedSeats List of fetched show seat DTOs
     */
    void validateSeatsActive(UUID showId, List<ShowSeatDto> fetchedSeats);

    /**
     * Validates that all fetched seats are in AVAILABLE status.
     *
     * @param showId Target show ID
     * @param fetchedSeats List of fetched show seat DTOs
     */
    void validateSeatsAvailable(UUID showId, List<ShowSeatDto> fetchedSeats);

    /**
     * Orchestrates complete seat validation (existence, show ownership, active status, availability).
     *
     * @param showId Target show ID
     * @param requestedSeatIds List of requested seat IDs
     * @param fetchedSeats List of fetched show seat DTOs
     */
    void validateSeats(UUID showId, List<UUID> requestedSeatIds, List<ShowSeatDto> fetchedSeats);
}
