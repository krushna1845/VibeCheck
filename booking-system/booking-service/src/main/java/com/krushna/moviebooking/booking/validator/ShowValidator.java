package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.client.ShowClient.ShowDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Validator interface for show existence, active status, and non-expiration.
 */
public interface ShowValidator {

    /**
     * Validates that the requested show exists.
     *
     * @param showOpt Optional show DTO returned from ShowClient
     * @param showId Requested show ID
     */
    void validateShowExists(Optional<ShowDto> showOpt, UUID showId);

    /**
     * Validates that the show status is active / scheduled.
     *
     * @param showDto Show DTO
     */
    void validateShowActive(ShowDto showDto);

    /**
     * Validates that the show start time has not already passed.
     *
     * @param showDto Show DTO
     */
    void validateShowNotExpired(ShowDto showDto);

    /**
     * Orchestrates complete show validation (existence, active status, expiration check).
     *
     * @param showOpt Optional show DTO returned from ShowClient
     * @param showId Requested show ID
     */
    void validateShow(Optional<ShowDto> showOpt, UUID showId);
}
