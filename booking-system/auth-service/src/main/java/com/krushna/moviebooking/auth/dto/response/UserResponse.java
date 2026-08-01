package com.krushna.moviebooking.auth.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String status,
        Boolean isEmailVerified,
        Set<String> roles,
        Instant createdAt
) {}
