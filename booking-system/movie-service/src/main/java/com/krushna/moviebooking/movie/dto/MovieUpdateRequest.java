package com.krushna.moviebooking.movie.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/**
 * Inbound DTO for partial or full update of an existing Movie.
 * All fields are optional — only non-null fields are applied (patch semantics).
 * Bean Validation still fires on any provided field.
 */
public record MovieUpdateRequest(

        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        String description,

        @Min(value = 1, message = "Duration must be greater than zero")
        Integer durationMinutes,

        LocalDate releaseDate,

        @Pattern(regexp = "U|UA|A|S", message = "Censor rating must be one of: U, UA, A, S")
        String censorRating,

        @Size(max = 512)
        String posterUrl,

        @Size(max = 512)
        String trailerUrl,

        Set<Integer> genreIds,

        Set<Integer> languageIds
) {}
