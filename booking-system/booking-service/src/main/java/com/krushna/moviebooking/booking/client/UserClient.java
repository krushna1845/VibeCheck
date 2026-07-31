package com.krushna.moviebooking.booking.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client interface for interacting with the User / Auth Service microservice.
 */
public interface UserClient {

    record UserDto(
            UUID id,
            String name,
            String email,
            String status, // ACTIVE, INACTIVE, SUSPENDED
            List<String> roles
    ) {}

    /**
     * Checks if a user exists.
     */
    boolean existsUser(UUID userId);

    /**
     * Fetches user details by user ID.
     */
    Optional<UserDto> getUserById(UUID userId);
}
