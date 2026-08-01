package com.krushna.moviebooking.theatre.controller;

import com.krushna.moviebooking.theatre.dto.CityRequest;
import com.krushna.moviebooking.theatre.dto.CityResponse;
import com.krushna.moviebooking.theatre.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
@Tag(name = "Cities", description = "Endpoints for managing cities")
public class CityController {

    private final CityService cityService;

    @Operation(summary = "Create a new city")
    @PostMapping
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
        log.info("REST POST /api/v1/cities name='{}'", request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(request));
    }

    @Operation(summary = "Get all cities")
    @GetMapping
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    @Operation(summary = "Get city by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CityResponse> getCityById(@PathVariable Integer id) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }
}
