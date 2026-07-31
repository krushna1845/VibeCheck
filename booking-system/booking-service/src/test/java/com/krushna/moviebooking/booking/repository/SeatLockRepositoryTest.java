package com.krushna.moviebooking.booking.repository;

import com.krushna.moviebooking.booking.config.SeatLockProperties;
import com.krushna.moviebooking.booking.model.SeatLock;
import com.krushna.moviebooking.booking.repository.impl.RedisSeatLockRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockRepositoryTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedisScript<Long> releaseLockScript;

    @Mock
    private RedisScript<Long> renewLockScript;

    @Spy
    private SeatLockProperties seatLockProperties = new SeatLockProperties();

    private SeatLockRepository seatLockRepository;

    private UUID showId;
    private UUID seatId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        seatLockRepository = new RedisSeatLockRepositoryImpl(
                stringRedisTemplate,
                seatLockProperties,
                releaseLockScript,
                renewLockScript
        );
        showId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("saveIfAbsent calls setIfAbsent on StringRedisTemplate")
    void saveIfAbsent_Success() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        SeatLock lock = SeatLock.builder()
                .showId(showId)
                .seatId(seatId)
                .userId(userId)
                .lockToken(UUID.randomUUID().toString())
                .lockedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .ttlSeconds(300)
                .build();

        boolean result = seatLockRepository.saveIfAbsent(lock, 300);

        assertThat(result).isTrue();
        String expectedKey = "seat:" + showId + ":" + seatId;
        verify(valueOperations).setIfAbsent(eq(expectedKey), anyString(), eq(Duration.ofSeconds(300)));
    }

    @Test
    @DisplayName("deleteIfOwnedBy executes Lua script")
    void deleteIfOwnedBy_Success() {
        String key = "seat:" + showId + ":" + seatId;
        when(stringRedisTemplate.execute(eq(releaseLockScript), eq(Collections.singletonList(key)), eq(userId.toString())))
                .thenReturn(1L);

        boolean result = seatLockRepository.deleteIfOwnedBy(showId, seatId, userId.toString());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("renewIfOwnedBy executes Lua script with TTL argument")
    void renewIfOwnedBy_Success() {
        String key = "seat:" + showId + ":" + seatId;
        when(stringRedisTemplate.execute(eq(renewLockScript), eq(Collections.singletonList(key)), eq(userId.toString()), eq("300")))
                .thenReturn(1L);

        boolean result = seatLockRepository.renewIfOwnedBy(showId, seatId, userId.toString(), 300);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("findById parses stored JSON lock value")
    void findById_ReturnsLock() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        String key = "seat:" + showId + ":" + seatId;
        String json = String.format("{\"showId\":\"%s\",\"seatId\":\"%s\",\"userId\":\"%s\",\"ttlSeconds\":300}", showId, seatId, userId);
        when(valueOperations.get(key)).thenReturn(json);

        Optional<SeatLock> lockOpt = seatLockRepository.findById(showId, seatId);

        assertThat(lockOpt).isPresent();
        assertThat(lockOpt.get().getUserId()).isEqualTo(userId);
    }
}
