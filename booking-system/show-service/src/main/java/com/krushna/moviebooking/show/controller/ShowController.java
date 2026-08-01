package com.krushna.moviebooking.show.controller;

import com.krushna.moviebooking.show.dto.ShowRequest;
import com.krushna.moviebooking.show.dto.ShowResponse;
import com.krushna.moviebooking.show.dto.ShowSeatResponse;
import com.krushna.moviebooking.show.dto.ShowUpdateRequest;
import com.krushna.moviebooking.show.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
@Tag(name = "Shows", description = "Endpoints for scheduling and querying movie shows")
public class ShowController {

    private final ShowService showService;

    @Operation(summary = "Create a new show schedule")
    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody ShowRequest request) {
        log.info("REST POST /api/v1/shows movieId={} screenId={}", request.movieId(), request.screenId());
        return ResponseEntity.status(HttpStatus.CREATED).body(showService.createShow(request));
    }

    @Operation(summary = "Get show by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable UUID id) {
        return ResponseEntity.ok(showService.getShowById(id));
    }

    @Operation(summary = "Get shows by movie ID")
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<Page<ShowResponse>> getShowsByMovie(
            @PathVariable UUID movieId,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId, pageable));
    }

    @Operation(summary = "Get shows by screen ID")
    @GetMapping("/screen/{screenId}")
    public ResponseEntity<Page<ShowResponse>> getShowsByScreen(
            @PathVariable UUID screenId,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(showService.getShowsByScreen(screenId, pageable));
    }

    @Operation(summary = "Get shows by date")
    @GetMapping("/date/{date}")
    public ResponseEntity<List<ShowResponse>> getShowsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showService.getShowsByDate(date));
    }

    @Operation(summary = "Get shows by theatre and date")
    @GetMapping("/theatre/{theatreId}/date/{date}")
    public ResponseEntity<List<ShowResponse>> getShowsByTheatreAndDate(
            @PathVariable UUID theatreId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showService.getShowsByTheatreAndDate(theatreId, date));
    }

    @Operation(summary = "Get show seats with availability and prices")
    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<ShowSeatResponse>> getShowSeats(@PathVariable UUID showId) {
        return ResponseEntity.ok(showService.getShowSeats(showId));
    }

    @Operation(summary = "Update show schedule")
    @PutMapping("/{id}")
    public ResponseEntity<ShowResponse> updateShow(
            @PathVariable UUID id, @Valid @RequestBody ShowUpdateRequest request) {
        return ResponseEntity.ok(showService.updateShow(id, request));
    }

    @Operation(summary = "Cancel a show")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ShowResponse> cancelShow(@PathVariable UUID id) {
        return ResponseEntity.ok(showService.cancelShow(id));
    }
}
