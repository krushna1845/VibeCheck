package com.krushna.moviebooking.common.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, String> validationErrors
) {}
