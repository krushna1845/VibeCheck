package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.repository.BookingRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockCleanupSchedulerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private BookingRepository bookingRepository;

    @Spy
    private SeatLockProperties seatLockProperties = new SeatLockProperties();

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private SeatLockCleanupScheduler seatLockCleanupScheduler;

    @Test
    @DisplayName("cleanupOrphanSeatLocks inspects Redis key count and updates active lock gauge")
    void cleanupOrphanSeatLocks_Success() {
        when(stringRedisTemplate.keys("seat:*")).thenReturn(Set.of("seat:1:1", "seat:1:2"));
        when(stringRedisTemplate.getExpire("seat:1:1")).thenReturn(300L);
        when(stringRedisTemplate.getExpire("seat:1:2")).thenReturn(250L);

        seatLockCleanupScheduler.cleanupOrphanSeatLocks();

        verify(stringRedisTemplate).keys("seat:*");
        assertThat(meterRegistry.find("seatlock.cleanup.runs").counter()).isNotNull();
        assertThat(meterRegistry.find("seatlock.cleanup.runs").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("cleanupOrphanSeatLocks detects and deletes orphan seat locks with TTL = -1")
    void cleanupOrphanSeatLocks_CleansOrphanLocks() {
        String key1 = "seat:1:1";
        String key2 = "seat:1:2";
        when(stringRedisTemplate.keys("seat:*")).thenReturn(Set.of(key1, key2));
        when(stringRedisTemplate.getExpire(key1)).thenReturn(-1L);
        when(stringRedisTemplate.getExpire(key2)).thenReturn(300L);

        seatLockCleanupScheduler.cleanupOrphanSeatLocks();

        verify(stringRedisTemplate).delete(key1);
        verify(stringRedisTemplate, never()).delete(key2);

        assertThat(meterRegistry.find("seatlock.orphan.cleaned").counter()).isNotNull();
        assertThat(meterRegistry.find("seatlock.orphan.cleaned").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("cleanupOrphanSeatLocks handles Redis exceptions gracefully")
    void cleanupOrphanSeatLocks_HandlesException() {
        when(stringRedisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis connection refused"));

        seatLockCleanupScheduler.cleanupOrphanSeatLocks();

        verify(stringRedisTemplate).keys("seat:*");
        assertThat(meterRegistry.find("seatlock.cleanup.runs").counter()).isNotNull();
    }
}
