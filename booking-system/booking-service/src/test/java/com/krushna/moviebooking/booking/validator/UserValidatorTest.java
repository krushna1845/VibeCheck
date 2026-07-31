package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.client.UserClient.UserDto;
import com.krushna.moviebooking.booking.exception.UserInactiveException;
import com.krushna.moviebooking.booking.exception.UserNotAuthorizedException;
import com.krushna.moviebooking.booking.exception.UserNotFoundException;
import com.krushna.moviebooking.booking.validator.impl.UserValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class UserValidatorTest {

    private UserValidatorImpl userValidator;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userValidator = new UserValidatorImpl();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("validateUser should pass for existing, active, authorized user")
    void validateUser_Valid() {
        UserDto userDto = new UserDto(userId, "Jane Doe", "jane@example.com", "ACTIVE", List.of("ROLE_CUSTOMER"));

        assertThatCode(() -> userValidator.validateUser(Optional.of(userDto), userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateUserExists should throw UserNotFoundException when empty")
    void validateUserExists_Empty() {
        assertThatThrownBy(() -> userValidator.validateUserExists(Optional.empty(), userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("validateUserActive should throw UserInactiveException when status is INACTIVE or SUSPENDED")
    void validateUserActive_Suspended() {
        UserDto userDto = new UserDto(userId, "Jane Doe", "jane@example.com", "SUSPENDED", List.of("ROLE_CUSTOMER"));

        assertThatThrownBy(() -> userValidator.validateUserActive(userDto))
                .isInstanceOf(UserInactiveException.class)
                .hasMessageContaining("is not active");
    }

    @Test
    @DisplayName("validateUserAuthorized should throw UserNotAuthorizedException when role is missing")
    void validateUserAuthorized_MissingRole() {
        UserDto userDto = new UserDto(userId, "Jane Doe", "jane@example.com", "ACTIVE", List.of("ROLE_GUEST"));

        assertThatThrownBy(() -> userValidator.validateUserAuthorized(userDto, "ROLE_CUSTOMER"))
                .isInstanceOf(UserNotAuthorizedException.class)
                .hasMessageContaining("is not authorized");
    }
}
