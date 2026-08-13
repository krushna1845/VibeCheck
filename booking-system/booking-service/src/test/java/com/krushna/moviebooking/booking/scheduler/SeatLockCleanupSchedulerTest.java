package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.repository.BookingRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockCleanupSchedulerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisKeyCommands redisKeyCommands;

    @Mock
    private Cursor<byte[]> cursor;

    @Mock
    private SeatLockProperties seatLockProperties;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private SeatLockCleanupScheduler seatLockCleanupScheduler;

    @BeforeEach
    void setUp() {
        lenient().when(redisConnection.keyCommands()).thenReturn(redisKeyCommands);
        lenient().when(seatLockProperties.getKeyPrefix()).thenReturn("seat:");
    }

    @Test
    @DisplayName("cleanupOrphanSeatLocks inspects Redis key count via SCAN and updates active lock gauge")
    void cleanupOrphanSeatLocks_Success() {
        List<byte[]> keys = List.of("seat:1:1".getBytes(StandardCharsets.UTF_8), "seat:1:2".getBytes(StandardCharsets.UTF_8));
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(keys.get(0), keys.get(1));
        when(redisKeyCommands.scan(any(ScanOptions.class))).thenReturn(cursor);

        doAnswer(invocation -> {
            RedisCallback<?> action = invocation.getArgument(0);
            return action.doInRedis(redisConnection);
        }).when(stringRedisTemplate).execute(any(RedisCallback.class));

        when(stringRedisTemplate.getExpire("seat:1:1")).thenReturn(300L);
        when(stringRedisTemplate.getExpire("seat:1:2")).thenReturn(250L);

        seatLockCleanupScheduler.cleanupOrphanSeatLocks();

        verify(redisKeyCommands).scan(any(ScanOptions.class));
        assertThat(meterRegistry.find("seatlock.cleanup.runs").counter()).isNotNull();
        assertThat(meterRegistry.find("seatlock.cleanup.runs").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("cleanupOrphanSeatLocks detects and deletes orphan seat locks with TTL = -1")
    void cleanupOrphanSeatLocks_CleansOrphanLocks() {
        String key1 = "seat:1:1";
        String key2 = "seat:1:2";
        List<byte[]> keys = List.of(key1.getBytes(StandardCharsets.UTF_8), key2.getBytes(StandardCharsets.UTF_8));

        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(keys.get(0), keys.get(1));
        when(redisKeyCommands.scan(any(ScanOptions.class))).thenReturn(cursor);

        doAnswer(invocation -> {
            RedisCallback<?> action = invocation.getArgument(0);
            return action.doInRedis(redisConnection);
        }).when(stringRedisTemplate).execute(any(RedisCallback.class));

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
        when(stringRedisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        seatLockCleanupScheduler.cleanupOrphanSeatLocks();

        assertThat(meterRegistry.find("seatlock.cleanup.runs").counter()).isNotNull();
    }
}
