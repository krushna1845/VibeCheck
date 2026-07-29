package com.krushna.moviebooking.booking.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client interface for interacting with the Show Service microservice.
 */
public interface ShowClient {

    record ShowDto(
            UUID id,
            UUID movieId,
            UUID theatreId,
            UUID screenId,
            String status
    ) {}

    record ShowSeatDto(
            UUID id,
            UUID showId,
            UUID seatId,
            String seatNumber,
            BigDecimal price,
            String status
    ) {}

    /**
     * Checks if a show exists and is active.
     */
    boolean existsShow(UUID showId);

    /**
     * Fetches details of a specific show.
     */
    Optional<ShowDto> getShowById(UUID showId);

    /**
     * Fetches show seats by their primary keys for a show.
     */
    List<ShowSeatDto> getShowSeatsByIds(UUID showId, List<UUID> showSeatIds);

    /**
     * Updates the status of show seats (e.g. from AVAILABLE to BOOKED or back).
     */
    void updateShowSeatsStatus(UUID showId, List<UUID> showSeatIds, String status);
}
