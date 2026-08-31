package com.krushna.moviebooking.show.exception;

import java.util.List;
import java.util.UUID;

/**
 * Thrown when requested show seats cannot be found for a given show.
 */
public class ShowSeatNotFoundException extends RuntimeException {

    private final UUID showId;
    private final List<UUID> seatIds;

    public ShowSeatNotFoundException(UUID showId, List<UUID> seatIds) {
        super("Show seats not found for show " + showId + ": " + seatIds);
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
