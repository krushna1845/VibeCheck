package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.exception.InvalidBookingOwnershipException;
import com.krushna.moviebooking.booking.model.SeatLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockOwnershipValidatorTest {

    private LockOwnershipValidator validator;
    private UUID userId;
    private UUID otherUserId;
    private UUID showId;
    private UUID seatId;

    @BeforeEach
    void setUp() {
        validator = new LockOwnershipValidator();
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        showId = UUID.randomUUID();
        seatId = UUID.randomUUID();
    }

    @Test
    @DisplayName("isOwner returns true when user IDs match")
    void isOwner_ReturnsTrue_WhenUserIdsMatch() {
        SeatLock lock = SeatLock.builder()
                .showId(showId)
                .seatId(seatId)
                .userId(userId)
                .build();

        assertThat(validator.isOwner(lock, userId)).isTrue();
    }

    @Test
    @DisplayName("isOwner returns false when user IDs mismatch")
    void isOwner_ReturnsFalse_WhenUserIdsMismatch() {
        SeatLock lock = SeatLock.builder()
                .showId(showId)
                .seatId(seatId)
                .userId(otherUserId)
                .build();

        assertThat(validator.isOwner(lock, userId)).isFalse();
    }

    @Test
    @DisplayName("validateOwnership throws InvalidBookingOwnershipException when lock is null")
    void validateOwnership_Throws_WhenLockIsNull() {
        assertThatThrownBy(() -> validator.validateOwnership(null, userId))
                .isInstanceOf(InvalidBookingOwnershipException.class)
                .hasMessageContaining("Seat lock does not exist");
    }

    @Test
    @DisplayName("validateOwnership throws InvalidBookingOwnershipException when user does not match")
    void validateOwnership_Throws_WhenUserMismatch() {
        SeatLock lock = SeatLock.builder()
                .showId(showId)
                .seatId(seatId)
                .userId(otherUserId)
                .build();

        assertThatThrownBy(() -> validator.validateOwnership(lock, userId))
                .isInstanceOf(InvalidBookingOwnershipException.class)
                .hasMessageContaining("does not own seat lock");
    }
}
