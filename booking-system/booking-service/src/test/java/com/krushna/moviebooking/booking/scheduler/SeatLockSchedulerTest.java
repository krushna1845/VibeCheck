package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockSchedulerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Spy
    private SeatLockProperties seatLockProperties = new SeatLockProperties();

    @InjectMocks
    private SeatLockScheduler seatLockScheduler;

    @Test
    @DisplayName("monitorActiveSeatLocks inspects Redis key count without throwing errors")
    void monitorActiveSeatLocks_Success() {
        when(stringRedisTemplate.keys("seat:*")).thenReturn(Set.of("seat:1:1", "seat:1:2"));

        seatLockScheduler.monitorActiveSeatLocks();

        verify(stringRedisTemplate).keys("seat:*");
    }

    @Test
    @DisplayName("monitorActiveSeatLocks handles Redis exceptions gracefully")
    void monitorActiveSeatLocks_HandlesException() {
        when(stringRedisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        seatLockScheduler.monitorActiveSeatLocks();

        verify(stringRedisTemplate).keys("seat:*");
    }
}
