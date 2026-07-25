package com.krushna.moviebooking.movie.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Set;

/**
 * Inbound DTO for creating a new Movie.
 * Validated at the controller boundary before reaching the service layer.
 */
@Builder
public record MovieRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        String description,

        @NotNull(message = "Duration in minutes is required")
        @Min(value = 1, message = "Duration must be greater than zero")
        Integer durationMinutes,

        @NotNull(message = "Release date is required")
        LocalDate releaseDate,

        @NotBlank(message = "Censor rating is required")
        @Pattern(regexp = "U|UA|A|S", message = "Censor rating must be one of: U, UA, A, S")
        String censorRating,

        @Size(max = 512)
        String posterUrl,

        @Size(max = 512)
        String trailerUrl,

        @NotEmpty(message = "At least one genre ID must be provided")
        Set<Integer> genreIds,

        @NotEmpty(message = "At least one language ID must be provided")
        Set<Integer> languageIds
) {}
