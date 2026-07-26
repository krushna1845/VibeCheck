package com.krushna.moviebooking.theatre.service;

import com.krushna.moviebooking.theatre.dto.CityRequest;
import com.krushna.moviebooking.theatre.dto.CityResponse;

import java.util.List;

/**
 * Contract for all City domain operations.
 */
public interface CityService {

    /**
     * Creates a new City after validating uniqueness of (name, state).
     *
     * @param request the create payload
     * @return the created CityResponse DTO
     */
    CityResponse createCity(CityRequest request);

    /**
     * Updates an existing City by ID.
     *
     * @param id      the City ID
     * @param request the update payload
     * @return the updated CityResponse DTO
     */
    CityResponse updateCity(Integer id, CityRequest request);

    /**
     * Retrieves a single City by primary key.
     *
     * @param id the City ID
     * @return the CityResponse DTO
     */
    CityResponse getCityById(Integer id);

    /**
     * Returns all Cities.
     *
     * @return list of CityResponse DTOs
     */
    List<CityResponse> getAllCities();

    /**
     * Searches Cities by name (case-insensitive partial match).
     *
     * @param name the search query
     * @return list of matching CityResponse DTOs
     */
    List<CityResponse> searchCities(String name);

    /**
     * Deletes a City by ID.
     *
     * @param id the City ID
     */
    void deleteCity(Integer id);
}
