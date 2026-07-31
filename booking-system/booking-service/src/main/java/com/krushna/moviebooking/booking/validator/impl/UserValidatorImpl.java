package com.krushna.moviebooking.booking.validator.impl;

import com.krushna.moviebooking.booking.client.UserClient.UserDto;
import com.krushna.moviebooking.booking.exception.UserInactiveException;
import com.krushna.moviebooking.booking.exception.UserNotAuthorizedException;
import com.krushna.moviebooking.booking.exception.UserNotFoundException;
import com.krushna.moviebooking.booking.validator.UserValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Primary implementation of {@link UserValidator}.
 */
@Slf4j
@Component
public class UserValidatorImpl implements UserValidator {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DEFAULT_REQUIRED_ROLE = "ROLE_CUSTOMER";

    @Override
    public void validateUserExists(Optional<UserDto> userOpt, UUID userId) {
        log.debug("Validating user existence for userId: {}", userId);
        if (userOpt.isEmpty()) {
            log.warn("User with ID {} does not exist", userId);
            throw new UserNotFoundException(userId);
        }
    }

    @Override
    public void validateUserActive(UserDto userDto) {
        log.debug("Validating user active state for userId: {}", userDto.id());
        if (userDto.status() == null || !ACTIVE_STATUS.equalsIgnoreCase(userDto.status())) {
            log.warn("User with ID {} is inactive with status: {}", userDto.id(), userDto.status());
            throw new UserInactiveException(userDto.id());
        }
    }

    @Override
    public void validateUserAuthorized(UserDto userDto, String requiredRole) {
        log.debug("Validating user authorization for userId: {}", userDto.id());
        String roleToCheck = requiredRole != null ? requiredRole : DEFAULT_REQUIRED_ROLE;
        if (userDto.roles() == null || !userDto.roles().contains(roleToCheck)) {
            log.warn("User with ID {} lacks required role: {}", userDto.id(), roleToCheck);
            throw new UserNotAuthorizedException(userDto.id(), "Missing required role: " + roleToCheck);
        }
    }

    @Override
    public void validateUser(Optional<UserDto> userOpt, UUID userId) {
        validateUserExists(userOpt, userId);
        UserDto userDto = userOpt.get();
        validateUserActive(userDto);
        validateUserAuthorized(userDto, DEFAULT_REQUIRED_ROLE);
    }
}
