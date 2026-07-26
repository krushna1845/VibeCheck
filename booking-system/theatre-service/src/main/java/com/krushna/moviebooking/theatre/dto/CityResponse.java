package com.krushna.moviebooking.theatre.dto;

import lombok.Builder;

/**
 * Outbound DTO for City entity representations.
 * Immutable Java 21 record.
 */
@Builder
public record CityResponse(
        Integer id,
        String name,
        String state,
        String country,
        String pincode
) {}
