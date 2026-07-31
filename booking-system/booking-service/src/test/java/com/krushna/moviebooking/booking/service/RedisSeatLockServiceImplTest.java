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
        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seat1, seat2))
                .userId(userId)
                .ttlSeconds(300)
                .build();

        when(seatLockRepository.saveIfAbsent(argThat(lock -> lock != null && seat1.equals(lock.getSeatId())), eq(300L))).thenReturn(true);
        when(seatLockRepository.saveIfAbsent(argThat(lock -> lock != null && seat2.equals(lock.getSeatId())), eq(300L))).thenReturn(false);

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isFalse();
        assertThat(response.failedSeatIds()).contains(seat2);
        verify(seatLockRepository).deleteIfOwnedBy(showId, seat1, userId.toString());
    }

    @Test
    @DisplayName("releaseLocks deletes all keys for target seats")
    void releaseLocks_Success() {
        seatLockService.releaseLocks(showId, List.of(seat1, seat2));

        verify(seatLockRepository).delete(showId, seat1);
        verify(seatLockRepository).delete(showId, seat2);
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
