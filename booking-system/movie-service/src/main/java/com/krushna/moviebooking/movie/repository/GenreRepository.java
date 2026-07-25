package com.krushna.moviebooking.movie.repository;

import com.krushna.moviebooking.movie.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    Optional<Genre> findByName(String name);

    Optional<Genre> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
