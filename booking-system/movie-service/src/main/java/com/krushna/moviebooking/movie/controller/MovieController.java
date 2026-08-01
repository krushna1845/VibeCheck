package com.krushna.moviebooking.movie.controller;

import com.krushna.moviebooking.movie.dto.MovieRequest;
import com.krushna.moviebooking.movie.dto.MovieResponse;
import com.krushna.moviebooking.movie.dto.MovieUpdateRequest;
import com.krushna.moviebooking.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Movie lifecycle management.
 *
 * <p>Base path: {@code /api/v1/movies}
 *
 * <p>All responses are paginated where applicable and wrapped in RFC 9457
 * ProblemDetail on error (handled by GlobalExceptionHandler).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Tag(name = "Movies", description = "Endpoints for managing movies — create, read, update, soft-delete and status transitions")
public class MovieController {

    private final MovieService movieService;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Operation(summary = "Create a new movie",
            description = "Creates a new movie with associated genres and languages. Title must be unique (case-insensitive).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movie created successfully",
                    content = @Content(schema = @Schema(implementation = MovieResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid genre/language ID",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "A movie with the same title already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieRequest request) {
        log.info("REST POST /api/v1/movies title='{}'", request.title());
        MovieResponse response = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Operation(summary = "Get movie by ID", description = "Retrieves a single movie by its UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie found"),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(
            @Parameter(description = "Movie UUID", required = true)
            @PathVariable UUID id) {
        log.debug("REST GET /api/v1/movies/{}", id);
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @Operation(summary = "List all movies",
            description = "Returns a paginated list of all movies. Supports sorting by title, releaseDate, status, createdAt.")
    @ApiResponse(responseCode = "200", description = "Page of movies returned")
    @GetMapping
    public ResponseEntity<Page<MovieResponse>> getAllMovies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.debug("REST GET /api/v1/movies page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(movieService.getAllMovies(pageable));
    }

    @Operation(summary = "Search movies",
            description = "Full-text search on title (case-insensitive partial match). Optional status filter (COMING_SOON, NOW_SHOWING, ENDED).")
    @ApiResponse(responseCode = "200", description = "Search results returned")
    @GetMapping("/search")
    public ResponseEntity<Page<MovieResponse>> searchMovies(
            @Parameter(description = "Title keyword to search")
            @RequestParam(required = false, defaultValue = "") String keyword,
            @Parameter(description = "Filter by status: COMING_SOON, NOW_SHOWING, ENDED")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        log.debug("REST GET /api/v1/movies/search keyword='{}' status='{}'", keyword, status);
        return ResponseEntity.ok(movieService.searchMovies(keyword, status, pageable));
    }

    @Operation(summary = "Get now-showing movies",
            description = "Returns paginated list of movies currently in NOW_SHOWING status.")
    @GetMapping("/now-showing")
    public ResponseEntity<Page<MovieResponse>> getNowShowing(
            @PageableDefault(size = 20, sort = "releaseDate", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("REST GET /api/v1/movies/now-showing");
        return ResponseEntity.ok(movieService.searchMovies("", "NOW_SHOWING", pageable));
    }

    @Operation(summary = "Get coming-soon movies",
            description = "Returns paginated list of movies in COMING_SOON status.")
    @GetMapping("/coming-soon")
    public ResponseEntity<Page<MovieResponse>> getComingSoon(
            @PageableDefault(size = 20, sort = "releaseDate", direction = Sort.Direction.ASC) Pageable pageable) {
        log.debug("REST GET /api/v1/movies/coming-soon");
        return ResponseEntity.ok(movieService.searchMovies("", "COMING_SOON", pageable));
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Operation(summary = "Update a movie",
            description = "Applies patch-semantics update — only non-null fields are modified.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie updated successfully"),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "New title conflicts with an existing movie",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @Parameter(description = "Movie UUID", required = true) @PathVariable UUID id,
            @Valid @RequestBody MovieUpdateRequest request) {
        log.info("REST PUT /api/v1/movies/{}", id);
        return ResponseEntity.ok(movieService.updateMovie(id, request));
    }

    @Operation(summary = "Change movie status",
            description = "Transitions the movie to a new lifecycle status. Allowed: COMING_SOON, NOW_SHOWING, ENDED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status value",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<MovieResponse> changeMovieStatus(
            @Parameter(description = "Movie UUID", required = true) @PathVariable UUID id,
            @Parameter(description = "Target status: COMING_SOON, NOW_SHOWING, ENDED", required = true)
            @RequestParam String status) {
        log.info("REST PATCH /api/v1/movies/{}/status status='{}'", id, status);
        return ResponseEntity.ok(movieService.changeMovieStatus(id, status));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Operation(summary = "Soft-delete a movie",
            description = "Marks the movie as deleted by setting deletedAt timestamp. No physical row removal.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Movie deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Movie not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "410", description = "Movie was already deleted",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(
            @Parameter(description = "Movie UUID", required = true) @PathVariable UUID id) {
        log.info("REST DELETE /api/v1/movies/{}", id);
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
