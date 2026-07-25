package com.krushna.moviebooking.movie.mapper;

import com.krushna.moviebooking.movie.dto.MovieResponse;
import com.krushna.moviebooking.movie.entity.Genre;
import com.krushna.moviebooking.movie.entity.Language;
import com.krushna.moviebooking.movie.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting between Movie entity and DTOs.
 *
 * componentModel = "spring" makes MapStruct generate a Spring bean
 * that can be injected anywhere without manual instantiation.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MovieMapper {

    /**
     * Converts a Movie entity to the outbound MovieResponse record.
     * MapStruct maps fields by name convention; genres and languages
     * are handled via the nested mapping methods below.
     */
    MovieResponse toResponse(Movie movie);

    /**
     * Maps a Genre entity to its compact summary projection.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "slug", source = "slug")
    MovieResponse.GenreSummary toGenreSummary(Genre genre);

    /**
     * Maps a Language entity to its compact summary projection.
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "code", source = "code")
    MovieResponse.LanguageSummary toLanguageSummary(Language language);
}
