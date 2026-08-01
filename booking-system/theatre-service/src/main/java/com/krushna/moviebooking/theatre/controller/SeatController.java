package com.krushna.moviebooking.theatre.controller;

import com.krushna.moviebooking.theatre.dto.SeatRequest;
import com.krushna.moviebooking.theatre.dto.SeatResponse;
import com.krushna.moviebooking.theatre.service.SeatService;
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
@Tag(name = "Seats", description = "Endpoints for managing seats within screens")
public class SeatController {

    private final SeatService seatService;

    @Operation(summary = "Create a seat in a screen")
    @PostMapping("/screens/{screenId}/seats")
    public ResponseEntity<SeatResponse> createSeat(
            @PathVariable UUID screenId, @Valid @RequestBody SeatRequest request) {
        log.info("REST POST /api/v1/screens/{}/seats row='{}' num={}", screenId, request.seatRow(), request.seatNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.createSeat(screenId, request));
    }

    @Operation(summary = "Create batch of seats in a screen")
    @PostMapping("/screens/{screenId}/seats/batch")
    public ResponseEntity<List<SeatResponse>> createSeatsBatch(
            @PathVariable UUID screenId, @Valid @RequestBody List<SeatRequest> requests) {
        log.info("REST POST /api/v1/screens/{}/seats/batch count={}", screenId, requests.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.createSeatsBatch(screenId, requests));
    }

    @Operation(summary = "Get seat by ID")
    @GetMapping("/seats/{id}")
    public ResponseEntity<SeatResponse> getSeatById(@PathVariable UUID id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @Operation(summary = "Get all seats for a screen")
    @GetMapping("/screens/{screenId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeatsByScreen(@PathVariable UUID screenId) {
        return ResponseEntity.ok(seatService.getSeatsByScreen(screenId));
    }

    @Operation(summary = "Get active seats for a screen")
    @GetMapping("/screens/{screenId}/seats/active")
    public ResponseEntity<List<SeatResponse>> getActiveSeatsByScreen(@PathVariable UUID screenId) {
        return ResponseEntity.ok(seatService.getActiveSeatsByScreen(screenId));
    }

    @Operation(summary = "Update a seat")
    @PutMapping("/seats/{id}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable UUID id, @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }

    @Operation(summary = "Toggle seat active status")
    @PatchMapping("/seats/{id}/status")
    public ResponseEntity<SeatResponse> toggleSeatStatus(
            @PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(seatService.toggleSeatStatus(id, active));
    }

    @Operation(summary = "Soft-delete (deactivate) a seat")
    @DeleteMapping("/seats/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable UUID id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}
