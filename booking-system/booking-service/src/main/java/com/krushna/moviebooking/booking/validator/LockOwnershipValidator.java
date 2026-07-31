package com.krushna.moviebooking.booking.validator;

import com.krushna.moviebooking.booking.exception.InvalidBookingOwnershipException;
import com.krushna.moviebooking.booking.model.SeatLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Component responsible for validating seat lock ownership against user identifiers and lock tokens.
 */
@Slf4j
@Component
public class LockOwnershipValidator {

    /**
     * Checks if the given seat lock belongs to the specified userId.
     *
     * @param seatLock Target seat lock entity
     * @param userId Requesting user UUID
     * @return true if ownership matches, false otherwise
     */
    public boolean isOwner(SeatLock seatLock, UUID userId) {
        if (seatLock == null || userId == null) {
            return false;
        }
        return Objects.equals(seatLock.getUserId(), userId);
    }

    /**
     * Checks if the given seat lock matches both the userId and lockToken (if provided).
     *
     * @param seatLock Target seat lock entity
     * @param userId Requesting user UUID
     * @param lockToken Lock authorization token
     * @return true if ownership matches, false otherwise
     */
    public boolean isOwner(SeatLock seatLock, UUID userId, String lockToken) {
        if (!isOwner(seatLock, userId)) {
            return false;
        }
        if (lockToken != null && !lockToken.isBlank() && seatLock.getLockToken() != null) {
            return Objects.equals(seatLock.getLockToken(), lockToken);
        }
        return true;
    }

    /**
     * Validates that the seat lock is owned by the specified user. Throws exception if invalid.
     *
     * @param seatLock Target seat lock entity
     * @param userId Requesting user UUID
     * @throws InvalidBookingOwnershipException if ownership validation fails
     */
    public void validateOwnership(SeatLock seatLock, UUID userId) {
        if (seatLock == null) {
            log.warn("Lock ownership validation failed: lock does not exist for userId: {}", userId);
            throw new InvalidBookingOwnershipException("Seat lock does not exist");
        }
        if (!isOwner(seatLock, userId)) {
            log.warn("User {} is not the owner of seat lock for showId: {}, seatId: {}",
                    userId, seatLock.getShowId(), seatLock.getSeatId());
            throw new InvalidBookingOwnershipException(
                    String.format("User %s does not own seat lock for show %s and seat %s",
                            userId, seatLock.getShowId(), seatLock.getSeatId()));
        }
        log.debug("Lock ownership validated successfully for userId: {}, showId: {}, seatId: {}",
                userId, seatLock.getShowId(), seatLock.getSeatId());
    }

    /**
     * Validates ownership matching both userId and optional lockToken.
     */
    public void validateOwnership(SeatLock seatLock, UUID userId, String lockToken) {
        validateOwnership(seatLock, userId);
        if (lockToken != null && !lockToken.isBlank() && seatLock.getLockToken() != null) {
            if (!Objects.equals(seatLock.getLockToken(), lockToken)) {
                log.warn("Lock token mismatch for userId: {}, expected: {}, actual: {}",
                        userId, seatLock.getLockToken(), lockToken);
                throw new InvalidBookingOwnershipException("Seat lock authorization token mismatch");
            }
        }
    }
}
