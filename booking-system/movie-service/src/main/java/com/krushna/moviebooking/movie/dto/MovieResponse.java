package com.krushna.moviebooking.movie.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Outbound DTO returned from every Movie service operation.
 * Uses Java 21 records for immutability — safe to cache and serialise.
 * Never exposes internal entity fields (e.g. deletedAt, password hashes).
 */
public record MovieResponse(

        UUID id,
        String title,
        String description,
        Integer durationMinutes,
        LocalDate releaseDate,
        String censorRating,
        String posterUrl,
        String trailerUrl,
        String status,
        Set<GenreSummary> genres,
        Set<LanguageSummary> languages,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Compact genre projection embedded in MovieResponse.
     */
    public record GenreSummary(Integer id, String name, String slug) {}

    /**
     * Compact language projection embedded in MovieResponse.
     */
    public record LanguageSummary(Integer id, String name, String code) {}
}
