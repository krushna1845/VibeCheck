package com.krushna.moviebooking.theatre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Inbound DTO for creating or updating a City.
 * Validated at the service boundary before persistence.
 */
@Builder
public record CityRequest(

        @NotBlank(message = "City name is required")
        @Size(max = 100, message = "City name must not exceed 100 characters")
        String name,

        @NotBlank(message = "State name is required")
        @Size(max = 100, message = "State name must not exceed 100 characters")
        String state,

        @Size(max = 100, message = "Country name must not exceed 100 characters")
        String country,

        @Size(max = 20, message = "Pincode must not exceed 20 characters")
        String pincode
) {}
