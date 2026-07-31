package com.krushna.moviebooking.booking.validator.impl;

import com.krushna.moviebooking.booking.client.ShowClient.ShowDto;
import com.krushna.moviebooking.booking.exception.ShowExpiredException;
import com.krushna.moviebooking.booking.exception.ShowInactiveException;
import com.krushna.moviebooking.booking.exception.ShowNotFoundException;
import com.krushna.moviebooking.booking.validator.ShowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Primary implementation of {@link ShowValidator}.
 */
@Slf4j
@Component
public class ShowValidatorImpl implements ShowValidator {

    private static final Set<String> ACTIVE_STATUSES = Set.of("SCHEDULED", "ACTIVE", "PUBLISHED", "OPEN");

    @Override
    public void validateShowExists(Optional<ShowDto> showOpt, UUID showId) {
        log.debug("Validating show existence for showId: {}", showId);
        if (showOpt.isEmpty()) {
            log.warn("Show with ID {} does not exist", showId);
            throw new ShowNotFoundException(showId);
        }
    }

    @Override
    public void validateShowActive(ShowDto showDto) {
        log.debug("Validating show active state for showId: {}", showDto.id());
        if (showDto.status() == null || !ACTIVE_STATUSES.contains(showDto.status().toUpperCase())) {
            log.warn("Show with ID {} is inactive with status: {}", showDto.id(), showDto.status());
            throw new ShowInactiveException(showDto.id(), showDto.status());
        }
    }

    @Override
    public void validateShowNotExpired(ShowDto showDto) {
        log.debug("Validating show start time for showId: {}", showDto.id());
        if (showDto.startTime() != null && showDto.startTime().isBefore(Instant.now())) {
            log.warn("Show with ID {} has already expired (start time: {})", showDto.id(), showDto.startTime());
            throw new ShowExpiredException(showDto.id(), showDto.startTime());
        }
    }

    @Override
    public void validateShow(Optional<ShowDto> showOpt, UUID showId) {
        validateShowExists(showOpt, showId);
        ShowDto showDto = showOpt.get();
        validateShowActive(showDto);
        validateShowNotExpired(showDto);
    }
}
