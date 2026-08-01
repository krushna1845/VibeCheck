package com.krushna.moviebooking.movie.controller;

import com.krushna.moviebooking.movie.entity.Language;
import com.krushna.moviebooking.movie.repository.LanguageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
@Tag(name = "Languages", description = "Endpoints for viewing movie languages")
public class LanguageController {

    private final LanguageRepository languageRepository;

    @Operation(summary = "List all languages")
    @GetMapping
    public ResponseEntity<List<Language>> getAllLanguages() {
        return ResponseEntity.ok(languageRepository.findAll());
    }

    @Operation(summary = "Get language by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Language> getLanguageById(@PathVariable Integer id) {
        return languageRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
