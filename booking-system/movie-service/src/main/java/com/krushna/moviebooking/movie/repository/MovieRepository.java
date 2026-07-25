package com.krushna.moviebooking.movie.repository;

import com.krushna.moviebooking.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    Page<Movie> findByStatus(String status, Pageable pageable);

    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Movie> findByStatusAndReleaseDateBefore(String status, LocalDate releaseDate, Pageable pageable);

    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g.slug = :genreSlug AND m.status = :status")
    Page<Movie> findByGenreSlugAndStatus(@Param("genreSlug") String genreSlug, @Param("status") String status, Pageable pageable);

    @Query("SELECT m FROM Movie m JOIN m.languages l WHERE l.code = :languageCode AND m.status = :status")
    Page<Movie> findByLanguageCodeAndStatus(@Param("languageCode") String languageCode, @Param("status") String status, Pageable pageable);

    // Used by MovieServiceImpl.searchMovies() when status filter is present
    Page<Movie> findByTitleContainingIgnoreCaseAndStatus(String title, String status, Pageable pageable);
}
