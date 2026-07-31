package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.client.ShowClient.ShowDto;
import com.krushna.moviebooking.booking.exception.ShowExpiredException;
import com.krushna.moviebooking.booking.exception.ShowInactiveException;
import com.krushna.moviebooking.booking.exception.ShowNotFoundException;
import com.krushna.moviebooking.booking.validator.impl.ShowValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ShowValidatorTest {

    private ShowValidatorImpl showValidator;
    private UUID showId;

    @BeforeEach
    void setUp() {
        showValidator = new ShowValidatorImpl();
        showId = UUID.randomUUID();
    }

    @Test
    @DisplayName("validateShow should pass for existing, active, non-expired show")
    void validateShow_Valid() {
        ShowDto showDto = new ShowDto(
                showId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SCHEDULED", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200)
        );

        assertThatCode(() -> showValidator.validateShow(Optional.of(showDto), showId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateShowExists should throw ShowNotFoundException when empty")
    void validateShowExists_Empty() {
        assertThatThrownBy(() -> showValidator.validateShowExists(Optional.empty(), showId))
                .isInstanceOf(ShowNotFoundException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("validateShowActive should throw ShowInactiveException when status is CANCELLED")
    void validateShowActive_Cancelled() {
        ShowDto showDto = new ShowDto(
                showId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "CANCELLED", Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200)
        );

        assertThatThrownBy(() -> showValidator.validateShowActive(showDto))
                .isInstanceOf(ShowInactiveException.class)
                .hasMessageContaining("is not active");
    }

    @Test
    @DisplayName("validateShowNotExpired should throw ShowExpiredException when startTime is in past")
    void validateShowNotExpired_Expired() {
        ShowDto showDto = new ShowDto(
                showId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SCHEDULED", Instant.now().minusSeconds(3600), Instant.now().minusSeconds(1800)
        );

        assertThatThrownBy(() -> showValidator.validateShowNotExpired(showDto))
                .isInstanceOf(ShowExpiredException.class)
                .hasMessageContaining("has expired");
    }
}
