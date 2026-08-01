package com.krushna.moviebooking.theatre.controller;

import com.krushna.moviebooking.theatre.dto.TheatreRequest;
import com.krushna.moviebooking.theatre.dto.TheatreResponse;
import com.krushna.moviebooking.theatre.dto.TheatreUpdateRequest;
import com.krushna.moviebooking.theatre.service.TheatreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/theatres")
@RequiredArgsConstructor
@Tag(name = "Theatres", description = "Endpoints for managing theatres")
public class TheatreController {

    private final TheatreService theatreService;

    @Operation(summary = "Create a new theatre")
    @PostMapping
    public ResponseEntity<TheatreResponse> createTheatre(@Valid @RequestBody TheatreRequest request) {
        log.info("REST POST /api/v1/theatres name='{}'", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(theatreService.createTheatre(request));
    }

    @Operation(summary = "Get theatre by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TheatreResponse> getTheatreById(@PathVariable UUID id) {
        return ResponseEntity.ok(theatreService.getTheatreById(id));
    }

    @Operation(summary = "List all theatres (paginated)")
    @GetMapping
    public ResponseEntity<Page<TheatreResponse>> getAllTheatres(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(theatreService.getAllTheatres(pageable));
    }

    @Operation(summary = "Get theatres by city ID")
    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<TheatreResponse>> getTheatresByCity(@PathVariable Integer cityId) {
        return ResponseEntity.ok(theatreService.getTheatresByCity(cityId));
    }

    @Operation(summary = "Search theatres by name")
    @GetMapping("/search")
    public ResponseEntity<Page<TheatreResponse>> searchTheatres(
            @RequestParam String keyword,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(theatreService.searchTheatres(keyword, pageable));
    }

    @Operation(summary = "Update a theatre")
    @PutMapping("/{id}")
    public ResponseEntity<TheatreResponse> updateTheatre(
            @PathVariable UUID id, @Valid @RequestBody TheatreUpdateRequest request) {
        return ResponseEntity.ok(theatreService.updateTheatre(id, request));
    }

    @Operation(summary = "Change theatre status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TheatreResponse> changeTheatreStatus(
            @PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(theatreService.changeTheatreStatus(id, status));
    }

    @Operation(summary = "Soft-delete a theatre")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTheatre(@PathVariable UUID id) {
        theatreService.deleteTheatre(id);
        return ResponseEntity.noContent().build();
    }
}
