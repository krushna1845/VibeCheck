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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSeatLockServiceImplTest {

    @Mock
    private SeatLockRepository seatLockRepository;

    @Spy
    private LockOwnershipValidator lockOwnershipValidator = new LockOwnershipValidator();

    @Spy
    private SeatLockProperties seatLockProperties = new SeatLockProperties();

    @InjectMocks
    private RedisSeatLockServiceImpl seatLockService;

    private UUID showId;
    private UUID seat1;
    private UUID seat2;
    private UUID userId;

    @BeforeEach
    void setUp() {
        showId = UUID.randomUUID();
        seat1 = UUID.randomUUID();
        seat2 = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("lockSeats successfully acquires all locks when seats are free")
    void lockSeats_Success() {
        when(seatLockRepository.saveIfAbsent(any(SeatLock.class), eq(300L))).thenReturn(true);

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seat1, seat2))
                .userId(userId)
                .bookingReference("BK123")
                .ttlSeconds(300)
                .build();

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isTrue();
        assertThat(response.lockedSeatIds()).containsExactlyInAnyOrder(seat1, seat2);
        assertThat(response.failedSeatIds()).isEmpty();
        verify(seatLockRepository, times(2)).saveIfAbsent(any(SeatLock.class), eq(300L));
    }

    @Test
    @DisplayName("lockSeats rolls back acquired seats when a subsequent seat lock fails")
    void lockSeats_RollbackOnConflict() {
        // Use deterministic UUIDs where seat1 sorts lexicographically before seat2,
        // so after sorting, seat1 is acquired first, then seat2 fails, triggering rollback.
        UUID seat1First = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID seat2Second = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seat1First, seat2Second))
                .userId(userId)
                .ttlSeconds(300)
                .build();

        when(seatLockRepository.saveIfAbsent(argThat(lock -> lock != null && seat1First.equals(lock.getSeatId())), eq(300L))).thenReturn(true);
        when(seatLockRepository.saveIfAbsent(argThat(lock -> lock != null && seat2Second.equals(lock.getSeatId())), eq(300L))).thenReturn(false);
        when(seatLockRepository.deleteIfOwnedBy(any(), any(), anyString())).thenReturn(true);

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isFalse();
        assertThat(response.failedSeatIds()).contains(seat2Second);
        // Rollback uses the lock token (not userId) for owner-verified atomic delete
        verify(seatLockRepository).deleteIfOwnedBy(eq(showId), eq(seat1First), anyString());
    }

    @Test
    @DisplayName("releaseLocks (unconditional) deletes all keys for target seats")
    void releaseLocks_Unconditional_DeletesAllSeats() {
        seatLockService.releaseLocks(showId, List.of(seat1, seat2));

        verify(seatLockRepository).delete(showId, seat1);
        verify(seatLockRepository).delete(showId, seat2);
    }

    @Test
    @DisplayName("releaseLocks(userId) delegates to deleteIfOwnedBy for each seat")
    void releaseLocks_OwnerVerified_ByUserId() {
        when(seatLockRepository.deleteIfOwnedBy(showId, seat1, userId.toString())).thenReturn(true);
        when(seatLockRepository.deleteIfOwnedBy(showId, seat2, userId.toString())).thenReturn(true);

        seatLockService.releaseLocks(showId, List.of(seat1, seat2), userId);

        verify(seatLockRepository).deleteIfOwnedBy(showId, seat1, userId.toString());
        verify(seatLockRepository).deleteIfOwnedBy(showId, seat2, userId.toString());
        verify(seatLockRepository, never()).delete(any(), any());
    }

    @Test
    @DisplayName("releaseLocks(userId) does NOT delete locks owned by a different user")
    void releaseLocks_OwnerVerified_DoesNotDeleteOtherOwnersLock() {
        // Lua script returns 0 (false) when owner mismatch
        when(seatLockRepository.deleteIfOwnedBy(showId, seat1, userId.toString())).thenReturn(false);

        seatLockService.releaseLocks(showId, List.of(seat1), userId);

        verify(seatLockRepository).deleteIfOwnedBy(showId, seat1, userId.toString());
        // Unconditional delete must never be called
        verify(seatLockRepository, never()).delete(any(), any());
    }

    @Test
    @DisplayName("releaseLock with userId delegates to deleteIfOwnedBy")
    void releaseLock_OwnerRestricted_Success() {
        when(seatLockRepository.deleteIfOwnedBy(showId, seat1, userId.toString())).thenReturn(true);

        boolean released = seatLockService.releaseLock(showId, seat1, userId);

        assertThat(released).isTrue();
        verify(seatLockRepository).deleteIfOwnedBy(showId, seat1, userId.toString());
    }

    @Test
    @DisplayName("renewLock extends TTL when caller is the owner")
    void renewLock_Success() {
        when(seatLockRepository.renewIfOwnedBy(showId, seat1, userId.toString(), 300L)).thenReturn(true);

        boolean renewed = seatLockService.renewLock(showId, seat1, userId, 300L);

        assertThat(renewed).isTrue();
        verify(seatLockRepository).renewIfOwnedBy(showId, seat1, userId.toString(), 300L);
    }

    @Test
    @DisplayName("validateOwnership returns true when user matches locked entity")
    void validateOwnership_Success() {
        SeatLock seatLock = SeatLock.builder()
                .showId(showId)
                .seatId(seat1)
                .userId(userId)
                .build();

        when(seatLockRepository.findById(showId, seat1)).thenReturn(Optional.of(seatLock));

        boolean isOwner = seatLockService.validateOwnership(showId, seat1, userId);

        assertThat(isOwner).isTrue();
    }

    @Test
    @DisplayName("isSeatLocked returns true when Redis key exists")
    void isSeatLocked_ReturnsTrue() {
        when(seatLockRepository.exists(showId, seat1)).thenReturn(true);

        boolean locked = seatLockService.isSeatLocked(showId, seat1);

        assertThat(locked).isTrue();
    }
}
