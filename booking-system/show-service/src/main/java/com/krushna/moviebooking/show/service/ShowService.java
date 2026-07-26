package com.krushna.moviebooking.show.service;

import com.krushna.moviebooking.show.dto.ShowRequest;
import com.krushna.moviebooking.show.dto.ShowResponse;
import com.krushna.moviebooking.show.dto.ShowSeatResponse;
import com.krushna.moviebooking.show.dto.ShowUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service contract for Show management operations.
 */
public interface ShowService {

    /**
     * Schedules a new show on a screen for a movie.
     * Enforces movie existence, screen existence, time range calculation (duration + cleaning buffer),
     * overlap prevention, and automatic creation of AVAILABLE ShowSeat records.
     *
     * @param request the Show creation payload
     * @return the created ShowResponse DTO
     */
    ShowResponse createShow(ShowRequest request);

    /**
     * Updates an existing show schedule or movie/screen assignment.
     * Re-calculates end time and re-evaluates overlap prevention.
     *
     * @param id      the Show UUID
     * @param request the update payload
     * @return the updated ShowResponse DTO
     */
    ShowResponse updateShow(UUID id, ShowUpdateRequest request);

    /**
     * Retrieves a single Show by ID.
     *
     * @param id the Show UUID
     * @return the ShowResponse DTO
     */
    ShowResponse getShowById(UUID id);

    /**
     * Cancels a scheduled Show if it has not already started.
     *
     * @param id the Show UUID
     * @return the cancelled ShowResponse DTO
     */
    ShowResponse cancelShow(UUID id);

    /**
     * Retrieves all active shows for a given movie.
     *
     * @param movieId the Movie UUID
     * @return list of ShowResponse DTOs
     */
    List<ShowResponse> getShowsByMovie(UUID movieId);

    /**
     * Retrieves paged active shows for a given movie.
     *
     * @param movieId  the Movie UUID
     * @param pageable pagination metadata
     * @return page of ShowResponse DTOs
     */
    Page<ShowResponse> getShowsByMovie(UUID movieId, Pageable pageable);

    /**
     * Retrieves all active shows for a given screen.
     *
     * @param screenId the Screen UUID
     * @return list of ShowResponse DTOs
     */
    List<ShowResponse> getShowsByScreen(UUID screenId);

    /**
     * Retrieves paged active shows for a given screen.
     *
     * @param screenId the Screen UUID
     * @param pageable pagination metadata
     * @return page of ShowResponse DTOs
     */
    Page<ShowResponse> getShowsByScreen(UUID screenId, Pageable pageable);

    /**
     * Retrieves all active shows scheduled for a specific date.
     *
     * @param date the target LocalDate
     * @return list of ShowResponse DTOs
     */
    List<ShowResponse> getShowsByDate(LocalDate date);

    /**
     * Retrieves all active shows scheduled for a specific theatre and date.
     *
     * @param theatreId the Theatre UUID
     * @param date      the target LocalDate
     * @return list of ShowResponse DTOs
     */
    List<ShowResponse> getShowsByTheatreAndDate(UUID theatreId, LocalDate date);

    /**
     * Retrieves all seat records for a specific show.
     *
     * @param showId the Show UUID
     * @return list of ShowSeatResponse DTOs
     */
    List<ShowSeatResponse> getShowSeats(UUID showId);
}
