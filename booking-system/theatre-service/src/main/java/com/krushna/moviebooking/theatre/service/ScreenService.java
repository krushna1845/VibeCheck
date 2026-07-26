package com.krushna.moviebooking.theatre.service;

import com.krushna.moviebooking.theatre.dto.ScreenRequest;
import com.krushna.moviebooking.theatre.dto.ScreenResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contract for Screen domain operations inside a Theatre.
 */
public interface ScreenService {

    /**
     * Creates a new Screen inside the specified Theatre.
     * Enforces uniqueness of screen name per theatre and seat capacity equal active seats.
     *
     * @param theatreId the parent Theatre UUID
     * @param request   the Screen creation payload
     * @return the created ScreenResponse DTO
     */
    ScreenResponse createScreen(UUID theatreId, ScreenRequest request);

    /**
     * Updates an existing Screen by ID.
     *
     * @param id      the Screen UUID
     * @param request the update payload
     * @return the updated ScreenResponse DTO
     */
    ScreenResponse updateScreen(UUID id, ScreenRequest request);

    /**
     * Retrieves a single Screen by ID.
     *
     * @param id the Screen UUID
     * @return the ScreenResponse DTO
     */
    ScreenResponse getScreenById(UUID id);

    /**
     * Returns all Screens belonging to a Theatre.
     *
     * @param theatreId the parent Theatre UUID
     * @return list of ScreenResponse DTOs
     */
    List<ScreenResponse> getScreensByTheatre(UUID theatreId);

    /**
     * Deletes a Screen by ID (soft-deactivates its seats).
     *
     * @param id the Screen UUID
     */
    void deleteScreen(UUID id);

    /**
     * Recalculates and updates the totalSeats capacity of a screen
     * to match its active seats count.
     *
     * @param screenId the Screen UUID
     * @return updated ScreenResponse DTO
     */
    ScreenResponse updateScreenCapacity(UUID screenId);
}
