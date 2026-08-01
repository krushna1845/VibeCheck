package com.krushna.moviebooking.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
        @NotBlank(message = "Token is required")
        String token
) {}
