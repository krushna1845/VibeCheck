package com.krushna.moviebooking.show.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client interface for interacting with the Theatre Service (Screen & Seats).
 */
public interface ScreenClient {

    record ScreenDto(
            UUID id,
            UUID theatreId,
            String name,
            String screenType,
            Integer totalSeats
    ) {}

    record SeatDto(
            UUID id,
            UUID screenId,
            String seatRow,
            Integer seatNumber,
            String seatCategory,
            Boolean isActive
    ) {}

    Optional<ScreenDto> getScreenById(UUID screenId);

    boolean existsScreen(UUID screenId);

    List<SeatDto> getActiveSeatsByScreen(UUID screenId);
}
