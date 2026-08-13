package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockSchedulerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SeatLockProperties seatLockProperties;

    @InjectMocks
    private SeatLockScheduler seatLockScheduler;

    @BeforeEach
    void setUp() {
        lenient().when(seatLockProperties.getKeyPrefix()).thenReturn("seat:");
    }

    @Test
    @DisplayName("monitorActiveSeatLocks inspects Redis key count without throwing errors using SCAN")
    void monitorActiveSeatLocks_Success() {
        RedisConnection connection = mock(RedisConnection.class);
        RedisKeyCommands keyCommands = mock(RedisKeyCommands.class);
        Cursor<byte[]> cursor = mock(Cursor.class);

        when(connection.keyCommands()).thenReturn(keyCommands);
        when(keyCommands.scan(any())).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("seat:1:1".getBytes(StandardCharsets.UTF_8), "seat:1:2".getBytes(StandardCharsets.UTF_8));

        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<Integer> callback = invocation.getArgument(0);
            return callback.doInRedis(connection);
        });

        seatLockScheduler.monitorActiveSeatLocks();

        verify(stringRedisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("monitorActiveSeatLocks handles Redis exceptions gracefully")
    void monitorActiveSeatLocks_HandlesException() {
        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenThrow(new RuntimeException("Redis connection refused"));

        seatLockScheduler.monitorActiveSeatLocks();

        verify(stringRedisTemplate).execute(any(RedisCallback.class));
    }
}
