package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.client.UserClient.UserDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Validator interface for user existence, active account state, and role authorization.
 */
public interface UserValidator {

    /**
     * Validates that the requested user exists.
     *
     * @param userOpt Optional user DTO returned from UserClient
     * @param userId Requested user ID
     */
    void validateUserExists(Optional<UserDto> userOpt, UUID userId);

    /**
     * Validates that the user account is active.
     *
     * @param userDto User DTO
     */
    void validateUserActive(UserDto userDto);

    /**
     * Validates that the user has the required permission / role.
     *
     * @param userDto User DTO
     * @param requiredRole Required role string (e.g. ROLE_CUSTOMER)
     */
    void validateUserAuthorized(UserDto userDto, String requiredRole);

    /**
     * Orchestrates complete user validation (existence, active state, role authorization).
     *
     * @param userOpt Optional user DTO returned from UserClient
     * @param userId Requested user ID
     */
    void validateUser(Optional<UserDto> userOpt, UUID userId);
}
