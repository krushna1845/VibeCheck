package com.krushna.moviebooking.theatre.service;

import com.krushna.moviebooking.theatre.dto.SeatRequest;
import com.krushna.moviebooking.theatre.dto.SeatResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contract for Seat domain operations inside a Screen.
 */
public interface SeatService {

    /**
     * Creates a single Seat for the given Screen.
     * Enforces seat number uniqueness within the screen and validates seat category.
     *
     * @param screenId the parent Screen UUID
     * @param request  the Seat creation payload
     * @return the created SeatResponse DTO
     */
    SeatResponse createSeat(UUID screenId, SeatRequest request);

    /**
     * Creates a batch of Seats for the given Screen.
     *
     * @param screenId the parent Screen UUID
     * @param requests list of Seat creation payloads
     * @return list of created SeatResponse DTOs
     */
    List<SeatResponse> createSeatsBatch(UUID screenId, List<SeatRequest> requests);

    /**
     * Updates an existing Seat.
     *
     * @param id      the Seat UUID
     * @param request the update payload
     * @return the updated SeatResponse DTO
     */
    SeatResponse updateSeat(UUID id, SeatRequest request);

    /**
     * Retrieves a single Seat by ID.
     *
     * @param id the Seat UUID
     * @return the SeatResponse DTO
     */
    SeatResponse getSeatById(UUID id);

    /**
     * Retrieves all Seats for a Screen.
     *
     * @param screenId the parent Screen UUID
     * @return list of SeatResponse DTOs
     */
    List<SeatResponse> getSeatsByScreen(UUID screenId);

    /**
     * Retrieves active Seats for a Screen.
     *
     * @param screenId the parent Screen UUID
     * @return list of active SeatResponse DTOs
     */
    List<SeatResponse> getActiveSeatsByScreen(UUID screenId);

    /**
     * Toggles the active status of a seat and updates the parent screen's capacity.
     *
     * @param id       the Seat UUID
     * @param isActive target active state
     * @return the updated SeatResponse DTO
     */
    SeatResponse toggleSeatStatus(UUID id, boolean isActive);

    /**
     * Soft-deletes (deactivates) a Seat by ID and updates the parent screen's capacity.
     *
     * @param id the Seat UUID
     */
    void deleteSeat(UUID id);
}
