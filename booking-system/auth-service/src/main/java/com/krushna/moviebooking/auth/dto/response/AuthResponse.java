package com.krushna.moviebooking.auth.dto.response;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {}
