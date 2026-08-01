package com.krushna.moviebooking.movie.controller;

import com.krushna.moviebooking.movie.entity.Genre;
import com.krushna.moviebooking.movie.repository.GenreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
@Tag(name = "Genres", description = "Endpoints for viewing movie genres")
public class GenreController {

    private final GenreRepository genreRepository;

    @Operation(summary = "List all genres")
    @GetMapping
    public ResponseEntity<List<Genre>> getAllGenres() {
        return ResponseEntity.ok(genreRepository.findAll());
    }

    @Operation(summary = "Get genre by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Genre> getGenreById(@PathVariable Integer id) {
        return genreRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
