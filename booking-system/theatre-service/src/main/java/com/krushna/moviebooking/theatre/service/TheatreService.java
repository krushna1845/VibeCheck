package com.krushna.moviebooking.theatre.service;

import com.krushna.moviebooking.theatre.dto.TheatreRequest;
import com.krushna.moviebooking.theatre.dto.TheatreResponse;
import com.krushna.moviebooking.theatre.dto.TheatreUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Contract for all Theatre domain operations.
 */
public interface TheatreService {

    /**
     * Creates a new Theatre after performing uniqueness validation within the city.
     *
     * @param request the create payload
     * @return the created TheatreResponse DTO
     */
    TheatreResponse createTheatre(TheatreRequest request);

    /**
     * Updates an existing Theatre with patch semantics.
     *
     * @param id      the Theatre UUID
     * @param request the update payload
     * @return the updated TheatreResponse DTO
     */
    TheatreResponse updateTheatre(UUID id, TheatreUpdateRequest request);

    /**
     * Soft-deletes a Theatre by setting status to DELETED and deletedAt timestamp.
     *
     * @param id the Theatre UUID
     */
    void deleteTheatre(UUID id);

    /**
     * Retrieves a single active Theatre by primary key.
     *
     * @param id the Theatre UUID
     * @return the TheatreResponse DTO
     */
    TheatreResponse getTheatreById(UUID id);

    /**
     * Returns a paginated list of all active (non-deleted) Theatres.
     *
     * @param pageable pagination parameters
     * @return page of TheatreResponse DTOs
     */
    Page<TheatreResponse> getAllTheatres(Pageable pageable);

    /**
     * Returns all active Theatres in a given City.
     *
     * @param cityId the City ID
     * @return list of TheatreResponse DTOs
     */
    List<TheatreResponse> getTheatresByCity(Integer cityId);

    /**
     * Searches active Theatres by name keyword.
     *
     * @param keyword  the search keyword
     * @param pageable pagination parameters
     * @return page of TheatreResponse DTOs
     */
    Page<TheatreResponse> searchTheatres(String keyword, Pageable pageable);

    /**
     * Changes the status of a Theatre (e.g. ACTIVE, INACTIVE, MAINTENANCE).
     *
     * @param id     the Theatre UUID
     * @param status the new status string
     * @return the updated TheatreResponse DTO
     */
    TheatreResponse changeTheatreStatus(UUID id, String status);
}
