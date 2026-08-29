package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.model.SeatLock;
import com.krushna.moviebooking.booking.repository.SeatLockRepository;
import com.krushna.moviebooking.booking.service.impl.RedisSeatLockServiceImpl;
import com.krushna.moviebooking.booking.validator.LockOwnershipValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Ownership-correctness test suite for {@link RedisSeatLockServiceImpl}.
 *
 * <p>Covers all 8 required ownership scenarios mandated by Milestone 14:
 * <ol>
 *   <li>User A acquires lock, User A releases → lock removed</li>
 *   <li>User A acquires lock, User B attempts release → lock remains (Lua returns 0)</li>
 *   <li>User A lock expires, User B acquires same seat, stale User A cleanup runs → User B lock intact</li>
 *   <li>User A owns multiple seats → only User A locks are released</li>
 *   <li>10 concurrent users race for one seat → exactly 1 winner</li>
 *   <li>Concurrent release/acquire race → no new owner's lock deleted</li>
 *   <li>Lock extension: correct owner → succeeds; wrong owner → fails</li>
 *   <li>Redis unavailable → fails safely, no false-positive reservation</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Redis Seat Lock Ownership Correctness Test Suite")
class RedisSeatLockOwnershipTest {

    @Mock
    private SeatLockRepository seatLockRepository;

    @Spy
    private LockOwnershipValidator lockOwnershipValidator = new LockOwnershipValidator();

    @Spy
    private SeatLockProperties seatLockProperties = new SeatLockProperties();

    @InjectMocks
    private RedisSeatLockServiceImpl seatLockService;

    private UUID showId;
    private UUID seatId;
    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        showId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Scenario 1: User A acquires lock, User A releases → lock removed
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 1: Owner releases own lock — deleteIfOwnedBy called and returns true")
    void scenario1_OwnerReleasesOwnLock() {
        when(seatLockRepository.saveIfAbsent(any(SeatLock.class), eq(300L))).thenReturn(true);
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seatId), anyString())).thenReturn(true);

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(userA)
                .bookingReference("BK-OWNER-REL")
                .ttlSeconds(300)
                .build();

        SeatLockResponse lockResponse = seatLockService.lockSeats(request);
        assertThat(lockResponse.success()).isTrue();
        assertThat(lockResponse.lockToken()).isNotBlank();

        // Owner A releases by lockToken
        seatLockService.releaseLocksByToken(showId, List.of(seatId), lockResponse.lockToken());

        // Lua-based deleteIfOwnedBy must be called — never unconditional delete
        verify(seatLockRepository, atLeastOnce()).deleteIfOwnedBy(eq(showId), eq(seatId), eq(lockResponse.lockToken()));
        verify(seatLockRepository, never()).delete(any(), any());
    }

    // -------------------------------------------------------------------------
    // Scenario 2: User A acquires lock, User B attempts release → lock remains
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 2: Non-owner release attempt — Lua returns false (0), lock left intact")
    void scenario2_NonOwnerReleaseAttemptDoesNotDeleteLock() {
        // Simulate: User B's token does NOT match stored lock → Lua returns 0 (false)
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seatId), eq(userB.toString()))).thenReturn(false);

        // User B tries to release User A's seat
        boolean released = seatLockService.releaseLock(showId, seatId, userB);

        assertThat(released).isFalse();
        verify(seatLockRepository).deleteIfOwnedBy(eq(showId), eq(seatId), eq(userB.toString()));
        verify(seatLockRepository, never()).delete(any(), any());
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Stale cleanup (User A expiration) must NOT delete User B's lock
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 3: Stale User A cleanup cannot wipe User B's newly acquired lock")
    void scenario3_StaleExpirationCannotWipeNewOwnersLock() {
        // User B has acquired the seat after User A's lock expired.
        // Stale cleanup for User A calls releaseLocks(showId, seatIds, userA).
        // The Lua script sees a different userId in Redis → returns 0 → lock stays.
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seatId), eq(userA.toString()))).thenReturn(false);

        // Stale cleanup runs with User A's userId
        seatLockService.releaseLocks(showId, List.of(seatId), userA);

        // Unconditional delete must NEVER be called
        verify(seatLockRepository, never()).delete(any(), any());
        // Owner-verified delete was attempted but rejected by Lua (simulated by returning false)
        verify(seatLockRepository).deleteIfOwnedBy(showId, seatId, userA.toString());
    }

    @Test
    @DisplayName("Scenario 3b: Stale token-based cleanup cannot wipe User B's newly acquired lock")
    void scenario3b_StaleTokenCleanupCannotWipeNewOwnersLock() {
        String staleTokenA = UUID.randomUUID().toString();
        // Lua rejects User A's stale token (User B now owns the seat)
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seatId), eq(staleTokenA))).thenReturn(false);

        seatLockService.releaseLocksByToken(showId, List.of(seatId), staleTokenA);

        verify(seatLockRepository, never()).delete(any(), any());
        verify(seatLockRepository).deleteIfOwnedBy(showId, seatId, staleTokenA);
    }

    // -------------------------------------------------------------------------
    // Scenario 4: User A owns multiple seats → only User A locks can be released
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 4: Batch release by token only touches locks with matching token")
    void scenario4_BatchReleaseByTokenOnlyReleasesMatchingLocks() {
        UUID seat2 = UUID.randomUUID();
        UUID seat3 = UUID.randomUUID();

        String tokenA = UUID.randomUUID().toString();
        // seat1 and seat2 owned by A → Lua returns 1; seat3 owned by B → Lua returns 0
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seatId), eq(tokenA))).thenReturn(true);
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seat2), eq(tokenA))).thenReturn(true);
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seat3), eq(tokenA))).thenReturn(false);

        seatLockService.releaseLocksByToken(showId, List.of(seatId, seat2, seat3), tokenA);

        verify(seatLockRepository).deleteIfOwnedBy(showId, seatId, tokenA);
        verify(seatLockRepository).deleteIfOwnedBy(showId, seat2, tokenA);
        verify(seatLockRepository).deleteIfOwnedBy(showId, seat3, tokenA);
        // seat3 was not deleted (returned false) — no unconditional fallback
        verify(seatLockRepository, never()).delete(any(), any());
    }

    // -------------------------------------------------------------------------
    // Scenario 5: 10 concurrent users race for one seat → exactly 1 winner
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 5: 10 concurrent users race for the same seat — exactly 1 wins")
    void scenario5_ConcurrentAcquisitionExactlyOneWinner() throws InterruptedException {
        int threadCount = 10;
        AtomicInteger winnerCount = new AtomicInteger(0);
        AtomicInteger firstCall = new AtomicInteger(0);

        // Only the very first saveIfAbsent call returns true; all subsequent calls return false
        when(seatLockRepository.saveIfAbsent(any(SeatLock.class), anyLong()))
                .thenAnswer(inv -> firstCall.incrementAndGet() == 1);

        // Use lenient since deleteIfOwnedBy is only called on rollback (non-winning threads),
        // and the exact number of rollback calls depends on thread scheduling.
        lenient().when(seatLockRepository.deleteIfOwnedBy(any(), any(), anyString())).thenReturn(true);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            UUID userId = UUID.randomUUID();
            executor.submit(() -> {
                try {
                    startLatch.await();
                    SeatLockRequest req = SeatLockRequest.builder()
                            .showId(showId)
                            .seatIds(List.of(seatId))
                            .userId(userId)
                            .ttlSeconds(300)
                            .build();
                    SeatLockResponse response = seatLockService.lockSeats(req);
                    if (response.success()) {
                        winnerCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdownNow();

        assertThat(winnerCount.get()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Concurrent release/acquire race → new owner's lock not deleted
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 6: Concurrent release/acquire race — new owner lock never deleted by stale release")
    void scenario6_ConcurrentReleaseAcquireRace() throws InterruptedException {
        // Simulate race: User A tries to release at the same moment User B acquires.
        // User A's release should use deleteIfOwnedBy with userA's token → returns false
        // because User B already set the lock. No unconditional delete is ever called.
        when(seatLockRepository.deleteIfOwnedBy(eq(showId), eq(seatId), eq(userA.toString()))).thenReturn(false);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> errors = new ArrayList<>();

        Thread releaseThread = new Thread(() -> {
            try {
                latch.await();
                seatLockService.releaseLocks(showId, List.of(seatId), userA);
            } catch (Exception e) {
                errors.add("release-thread: " + e.getMessage());
            }
        });
        releaseThread.start();
        latch.countDown();
        releaseThread.join();

        // Verify: unconditional delete was never called; new owner's lock remains safe
        verify(seatLockRepository, never()).delete(any(), any());
        assertThat(errors).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Lock extension — correct owner succeeds, wrong owner fails
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 7a: Lock extension by correct owner succeeds")
    void scenario7a_LockExtensionByCorrectOwnerSucceeds() {
        when(seatLockRepository.renewIfOwnedBy(eq(showId), eq(seatId), eq(userA.toString()), eq(300L))).thenReturn(true);

        boolean renewed = seatLockService.renewLock(showId, seatId, userA, 300L);

        assertThat(renewed).isTrue();
        verify(seatLockRepository).renewIfOwnedBy(showId, seatId, userA.toString(), 300L);
    }

    @Test
    @DisplayName("Scenario 7b: Lock extension by wrong owner fails — renewIfOwnedBy returns false")
    void scenario7b_LockExtensionByWrongOwnerFails() {
        // Lua returns 0 (false) when token mismatch
        when(seatLockRepository.renewIfOwnedBy(eq(showId), eq(seatId), eq(userB.toString()), eq(300L))).thenReturn(false);

        boolean renewed = seatLockService.renewLock(showId, seatId, userB, 300L);

        assertThat(renewed).isFalse();
        verify(seatLockRepository).renewIfOwnedBy(showId, seatId, userB.toString(), 300L);
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Redis unavailable → fails safely, no false-positive reservation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Scenario 8a: Redis unavailable during lock acquisition — returns failure, not false success")
    void scenario8a_RedisUnavailableDuringAcquisition_ReturnsFalse() {
        // Redis throws an exception (connection refused, timeout, etc.)
        when(seatLockRepository.saveIfAbsent(any(SeatLock.class), anyLong()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(userA)
                .ttlSeconds(300)
                .build();

        // The repository impl catches Redis exceptions and returns false → lockSeats reports failure
        SeatLockResponse response = seatLockService.lockSeats(request);

        // Must NOT report success when Redis did not confirm the lock
        assertThat(response.success()).isFalse();
        assertThat(response.lockToken()).isNull();
    }

    @Test
    @DisplayName("Scenario 8b: Redis unavailable during release — error logged, exception does not propagate")
    void scenario8b_RedisUnavailableDuringRelease_DoesNotThrow() {
        // Redis throws during deleteIfOwnedBy — error must be observable/logged but not rethrown
        when(seatLockRepository.deleteIfOwnedBy(any(), any(), anyString()))
                .thenThrow(new RuntimeException("Redis connection lost"));

        // releaseLocksByToken catches exceptions per-seat and logs — must not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                seatLockService.releaseLocksByToken(showId, List.of(seatId), UUID.randomUUID().toString())
        );
    }

    @Test
    @DisplayName("Scenario 8c: Redis unavailable during release(userId) — error logged, exception does not propagate")
    void scenario8c_RedisUnavailableDuringUserIdRelease_DoesNotThrow() {
        when(seatLockRepository.deleteIfOwnedBy(any(), any(), anyString()))
                .thenThrow(new RuntimeException("Redis connection lost"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                seatLockService.releaseLocks(showId, List.of(seatId), userA)
        );
    }

    // -------------------------------------------------------------------------
    // Additional: lockToken is returned in response and is non-null on success
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("lockSeats returns a non-blank lockToken on success")
    void lockSeats_ReturnsNonBlankLockTokenOnSuccess() {
        when(seatLockRepository.saveIfAbsent(any(SeatLock.class), eq(300L))).thenReturn(true);

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(userA)
                .ttlSeconds(300)
                .build();

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isTrue();
        assertThat(response.lockToken()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("lockSeats returns null lockToken on failure")
    void lockSeats_ReturnsNullLockTokenOnFailure() {
        // First seat fails immediately
        when(seatLockRepository.saveIfAbsent(any(SeatLock.class), eq(300L))).thenReturn(false);

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(userA)
                .ttlSeconds(300)
                .build();

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isFalse();
        assertThat(response.lockToken()).isNull();
    }

    @Test
    @DisplayName("rollback on partial failure uses lockToken (not userId) for deleteIfOwnedBy")
    void lockSeats_RollbackUsesLockTokenNotUserId() {
        // Use deterministic UUIDs: seat1Sorted sorts first, seat2Sorted sorts second.
        // This guarantees seat1Sorted is acquired first, then seat2Sorted fails, triggering rollback.
        UUID seat1Sorted = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID seat2Sorted = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        when(seatLockRepository.saveIfAbsent(argThat(l -> l != null && seat1Sorted.equals(l.getSeatId())), eq(300L))).thenReturn(true);
        when(seatLockRepository.saveIfAbsent(argThat(l -> l != null && seat2Sorted.equals(l.getSeatId())), eq(300L))).thenReturn(false);
        when(seatLockRepository.deleteIfOwnedBy(any(), any(), anyString())).thenReturn(true);

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seat1Sorted, seat2Sorted))
                .userId(userA)
                .ttlSeconds(300)
                .build();

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isFalse();
        // Rollback must use deleteIfOwnedBy (token-based), never unconditional delete
        verify(seatLockRepository, never()).delete(any(), any());
        // Rollback was called with a token that is NOT the userId string (it's a UUID lockToken)
        verify(seatLockRepository).deleteIfOwnedBy(eq(showId), eq(seat1Sorted),
                argThat(token -> token != null && !token.equals(userA.toString())));
    }
}
