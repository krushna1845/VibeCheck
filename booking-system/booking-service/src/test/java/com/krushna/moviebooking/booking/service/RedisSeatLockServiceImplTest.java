package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.service.impl.RedisSeatLockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSeatLockServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

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
    @DisplayName("lockSeats successfully locks all requested seats when available")
    void lockSeats_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);

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
        verify(valueOperations, times(2)).setIfAbsent(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("lockSeats rolls back acquired seats when a subsequent seat is already locked")
    void lockSeats_RollbackOnPartialFailure() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        String key1 = "seat:" + showId + ":" + seat1;
        String key2 = "seat:" + showId + ":" + seat2;

        when(valueOperations.setIfAbsent(eq(key1), any(), any(Duration.class))).thenReturn(true);
        when(valueOperations.setIfAbsent(eq(key2), any(), any(Duration.class))).thenReturn(false);

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seat1, seat2))
                .userId(userId)
                .ttlSeconds(300)
                .build();

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isFalse();
        assertThat(response.failedSeatIds()).contains(seat2);
        // Verify rollback happened for seat1
        verify(stringRedisTemplate).delete(key1);
    }

    @Test
    @DisplayName("releaseLocks deletes keys for specified seats")
    void releaseLocks_Success() {
        seatLockService.releaseLocks(showId, List.of(seat1, seat2));

        verify(stringRedisTemplate).delete("seat:" + showId + ":" + seat1);
        verify(stringRedisTemplate).delete("seat:" + showId + ":" + seat2);
    }

    @Test
    @DisplayName("isSeatLocked returns true when Redis key exists")
    void isSeatLocked_ReturnsTrue() {
        String key = "seat:" + showId + ":" + seat1;
        when(stringRedisTemplate.hasKey(key)).thenReturn(true);

        boolean locked = seatLockService.isSeatLocked(showId, seat1);

        assertThat(locked).isTrue();
    }
}
