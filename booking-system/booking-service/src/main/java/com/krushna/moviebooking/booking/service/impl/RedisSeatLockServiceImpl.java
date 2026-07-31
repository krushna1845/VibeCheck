package com.krushna.moviebooking.booking.service.impl;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.model.SeatLock;
import com.krushna.moviebooking.booking.repository.SeatLockRepository;
import com.krushna.moviebooking.booking.service.SeatLockService;
import com.krushna.moviebooking.booking.validator.LockOwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * High-performance, distributed-lock-safe Redis implementation of {@link SeatLockService}.
 *
 * <p>Key format: {@code seat:{showId}:{seatId}}
 * <p>Default TTL: 300 seconds (5 minutes)
 * <p>Strictly operates in-memory on Redis — no database writes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSeatLockServiceImpl implements SeatLockService {

    private final SeatLockRepository seatLockRepository;
    private final LockOwnershipValidator lockOwnershipValidator;
    private final SeatLockProperties seatLockProperties;

    @Override
    public SeatLockResponse lockSeats(SeatLockRequest request) {
        UUID showId = request.showId();
        List<UUID> seatIds = request.seatIds();
        long ttlSeconds = request.ttlSeconds() > 0
                ? request.ttlSeconds()
                : (seatLockProperties != null ? seatLockProperties.getDefaultTtlSeconds() : 300L);

        log.info("Attempting to acquire distributed Redis seat locks for showId: {}, seatCount: {}, userId: {}, ttlSeconds: {}",
                showId, seatIds != null ? seatIds.size() : 0, request.userId(), ttlSeconds);

        if (showId == null || seatIds == null || seatIds.isEmpty()) {
            log.warn("Invalid lock request: showId or seatIds list is null/empty");
            return SeatLockResponse.builder()
                    .success(false)
                    .showId(showId)
                    .lockedSeatIds(List.of())
                    .failedSeatIds(List.of())
                    .message("Invalid lock request parameters")
                    .build();
        }

        List<UUID> acquiredSeats = new ArrayList<>();
        List<UUID> failedSeats = new ArrayList<>();
        String lockToken = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        for (UUID seatId : seatIds) {
            SeatLock seatLock = SeatLock.builder()
                    .showId(showId)
                    .seatId(seatId)
                    .userId(request.userId())
                    .lockToken(lockToken)
                    .bookingReference(request.bookingReference() != null ? request.bookingReference() : "N/A")
                    .lockedAt(now)
                    .expiresAt(expiresAt)
                    .ttlSeconds(ttlSeconds)
                    .build();

            boolean acquired = seatLockRepository.saveIfAbsent(seatLock, ttlSeconds);
            if (acquired) {
                acquiredSeats.add(seatId);
                log.debug("Successfully locked seatId: {} for showId: {}", seatId, showId);
            } else {
                failedSeats.add(seatId);
                log.warn("Failed to acquire lock for seatId: {} on showId: {} (seat already locked)", seatId, showId);
                break; // Stop immediately to preserve batch atomicity
            }
        }

        // If any seat lock failed, rollback acquired locks to maintain all-or-nothing atomicity
        if (!failedSeats.isEmpty()) {
            log.warn("Batch seat lock incomplete. Rolling back {} acquired seat locks for showId: {}", acquiredSeats.size(), showId);
            for (UUID acquiredSeatId : acquiredSeats) {
                seatLockRepository.deleteIfOwnedBy(showId, acquiredSeatId, request.userId().toString());
            }
            return SeatLockResponse.builder()
                    .success(false)
                    .showId(showId)
                    .lockedSeatIds(List.of())
                    .failedSeatIds(failedSeats)
                    .message("One or more requested seats are already locked by another customer")
                    .build();
        }

        log.info("Successfully acquired Redis seat locks for all {} seats on showId: {}, userId: {}, expiresAt: {}",
                acquiredSeats.size(), showId, request.userId(), expiresAt);

        return SeatLockResponse.builder()
                .success(true)
                .showId(showId)
                .lockedSeatIds(acquiredSeats)
                .failedSeatIds(List.of())
                .expiresAt(expiresAt)
                .message("Seats locked successfully")
                .build();
    }

    @Override
    public void releaseLocks(UUID showId, List<UUID> seatIds) {
        log.info("Releasing Redis seat locks for showId: {}, seatCount: {}", showId, seatIds != null ? seatIds.size() : 0);
        if (showId == null || seatIds == null || seatIds.isEmpty()) {
            return;
        }
        for (UUID seatId : seatIds) {
            seatLockRepository.delete(showId, seatId);
        }
    }

    @Override
    public boolean releaseLock(UUID showId, UUID seatId, UUID userId) {
        log.info("Attempting owner-restricted lock release for showId: {}, seatId: {}, userId: {}", showId, seatId, userId);
        if (showId == null || seatId == null || userId == null) {
            return false;
        }
        boolean released = seatLockRepository.deleteIfOwnedBy(showId, seatId, userId.toString());
        if (released) {
            log.info("Owner-restricted lock released successfully for showId: {}, seatId: {}, userId: {}", showId, seatId, userId);
        } else {
            log.warn("Owner-restricted lock release failed (key missing or owned by different user) for showId: {}, seatId: {}, userId: {}",
                    showId, seatId, userId);
        }
        return released;
    }

    @Override
    public boolean renewLock(UUID showId, UUID seatId, UUID userId, long ttlSeconds) {
        long effectiveTtl = ttlSeconds > 0
                ? ttlSeconds
                : (seatLockProperties != null ? seatLockProperties.getDefaultTtlSeconds() : 300L);

        log.info("Renewing seat lock for showId: {}, seatId: {}, userId: {}, newTtlSeconds: {}",
                showId, seatId, userId, effectiveTtl);

        if (showId == null || seatId == null || userId == null) {
            return false;
        }

        boolean renewed = seatLockRepository.renewIfOwnedBy(showId, seatId, userId.toString(), effectiveTtl);
        if (renewed) {
            log.info("Seat lock renewed successfully for showId: {}, seatId: {}, userId: {}", showId, seatId, userId);
        } else {
            log.warn("Seat lock renewal failed (lock expired or owned by another user) for showId: {}, seatId: {}, userId: {}",
                    showId, seatId, userId);
        }
        return renewed;
    }

    @Override
    public boolean renewLocks(UUID showId, List<UUID> seatIds, UUID userId, long ttlSeconds) {
        log.info("Batch renewing seat locks for showId: {}, seatCount: {}, userId: {}", showId, seatIds != null ? seatIds.size() : 0, userId);
        if (showId == null || seatIds == null || seatIds.isEmpty() || userId == null) {
            return false;
        }
        boolean allRenewed = true;
        for (UUID seatId : seatIds) {
            boolean renewed = renewLock(showId, seatId, userId, ttlSeconds);
            if (!renewed) {
                allRenewed = false;
            }
        }
        return allRenewed;
    }

    @Override
    public boolean validateOwnership(UUID showId, UUID seatId, UUID userId) {
        log.debug("Validating lock ownership for showId: {}, seatId: {}, userId: {}", showId, seatId, userId);
        Optional<SeatLock> seatLock = seatLockRepository.findById(showId, seatId);
        if (seatLock.isEmpty()) {
            return false;
        }
        return lockOwnershipValidator.isOwner(seatLock.get(), userId);
    }

    @Override
    public boolean isSeatLocked(UUID showId, UUID seatId) {
        log.debug("Checking lock status for showId: {}, seatId: {}", showId, seatId);
        if (showId == null || seatId == null) {
            return false;
        }
        return seatLockRepository.exists(showId, seatId);
    }

    @Override
    public Optional<SeatLock> getSeatLock(UUID showId, UUID seatId) {
        log.debug("Retrieving seat lock entity for showId: {}, seatId: {}", showId, seatId);
        if (showId == null || seatId == null) {
            return Optional.empty();
        }
        return seatLockRepository.findById(showId, seatId);
    }
}
