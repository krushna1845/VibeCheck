package com.krushna.moviebooking.auth.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record TokenValidationResponse(
        boolean valid,
        UUID userId,
        String email,
        List<String> roles
) {}
