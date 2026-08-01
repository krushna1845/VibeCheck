package com.krushna.moviebooking.theatre.controller;

import com.krushna.moviebooking.theatre.dto.ScreenRequest;
import com.krushna.moviebooking.theatre.dto.ScreenResponse;
import com.krushna.moviebooking.theatre.service.ScreenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Screens", description = "Endpoints for managing screens within theatres")
public class ScreenController {

    private final ScreenService screenService;

    @Operation(summary = "Create a screen in a theatre")
    @PostMapping("/theatres/{theatreId}/screens")
    public ResponseEntity<ScreenResponse> createScreen(
            @PathVariable UUID theatreId, @Valid @RequestBody ScreenRequest request) {
        log.info("REST POST /api/v1/theatres/{}/screens name='{}'", theatreId, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(screenService.createScreen(theatreId, request));
    }

    @Operation(summary = "Get screen by ID")
    @GetMapping("/screens/{id}")
    public ResponseEntity<ScreenResponse> getScreenById(@PathVariable UUID id) {
        return ResponseEntity.ok(screenService.getScreenById(id));
    }

    @Operation(summary = "Get screens by theatre ID")
    @GetMapping("/theatres/{theatreId}/screens")
    public ResponseEntity<List<ScreenResponse>> getScreensByTheatre(@PathVariable UUID theatreId) {
        return ResponseEntity.ok(screenService.getScreensByTheatre(theatreId));
    }

    @Operation(summary = "Update a screen")
    @PutMapping("/screens/{id}")
    public ResponseEntity<ScreenResponse> updateScreen(
            @PathVariable UUID id, @Valid @RequestBody ScreenRequest request) {
        return ResponseEntity.ok(screenService.updateScreen(id, request));
    }

    @Operation(summary = "Delete a screen")
    @DeleteMapping("/screens/{id}")
    public ResponseEntity<Void> deleteScreen(@PathVariable UUID id) {
        screenService.deleteScreen(id);
        return ResponseEntity.noContent().build();
    }
}
