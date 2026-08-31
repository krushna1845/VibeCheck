package com.krushna.moviebooking.show.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when attempting to book/confirm seats that are already BOOKED or unavailable.
 */
public class SeatAlreadyBookedException extends RuntimeException {

    private final UUID showId;
    private final List<UUID> seatIds;

    public SeatAlreadyBookedException(UUID showId, List<UUID> seatIds) {
        super("One or more requested seats are already booked or unavailable for show " + showId + ": " + seatIds);
        this.showId = showId;
        this.seatIds = seatIds;
    }

    public UUID getShowId() {
        return showId;
    }

    public List<UUID> getSeatIds() {
        return seatIds;
    }
}
