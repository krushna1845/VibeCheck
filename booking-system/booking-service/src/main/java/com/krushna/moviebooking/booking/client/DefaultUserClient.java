package com.krushna.moviebooking.booking.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary component implementation of {@link UserClient}.
 */
@Slf4j
@Component
public class DefaultUserClient implements UserClient {

    @Override
    public boolean existsUser(UUID userId) {
        log.debug("Checking existence of userId: {}", userId);
        return userId != null;
    }

    @Override
    public Optional<UserDto> getUserById(UUID userId) {
        log.debug("Fetching user details for userId: {}", userId);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.of(new UserDto(
                userId,
                "John Doe",
                "john.doe@example.com",
                "ACTIVE",
                List.of("ROLE_CUSTOMER")
        ));
    }
}
