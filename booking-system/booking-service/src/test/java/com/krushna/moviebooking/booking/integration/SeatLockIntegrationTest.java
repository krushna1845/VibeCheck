package com.krushna.moviebooking.booking.integration;

import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.service.SeatLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SeatLockIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        // Disable Flyway / Postgres for light-weight context startup if needed
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Integration: Acquire lock creates Redis key with expected format seat:{showId}:{seatId} and TTL 300s")
    void testAcquireLock_RedisKeyAndTTL() {
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        SeatLockRequest request = SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(userId)
                .bookingReference("BK-TEST-101")
                .ttlSeconds(300)
                .build();

        SeatLockResponse response = seatLockService.lockSeats(request);

        assertThat(response.success()).isTrue();
        assertThat(response.lockedSeatIds()).contains(seatId);

        String redisKey = "seat:" + showId + ":" + seatId;
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        assertThat(exists).isTrue();

        Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        assertThat(ttl).isGreaterThan(0L).isLessThanOrEqualTo(300L);
    }

    @Test
    @DisplayName("Integration: High Concurrency - Two users acquiring same seat simultaneously, exactly one succeeds")
    void testConcurrentLockAcquisition() throws InterruptedException, ExecutionException {
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<SeatLockResponse> task1 = () -> {
            barrier.await();
            return seatLockService.lockSeats(SeatLockRequest.builder()
                    .showId(showId)
                    .seatIds(List.of(seatId))
                    .userId(user1)
                    .ttlSeconds(300)
                    .build());
        };

        Callable<SeatLockResponse> task2 = () -> {
            barrier.await();
            return seatLockService.lockSeats(SeatLockRequest.builder()
                    .showId(showId)
                    .seatIds(List.of(seatId))
                    .userId(user2)
                    .ttlSeconds(300)
                    .build());
        };

        Future<SeatLockResponse> future1 = executor.submit(task1);
        Future<SeatLockResponse> future2 = executor.submit(task2);

        SeatLockResponse res1 = future1.get();
        SeatLockResponse res2 = future2.get();

        executor.shutdown();

        // Exactly one should succeed, one should fail
        boolean exactlyOneSuccess = (res1.success() && !res2.success()) || (!res1.success() && res2.success());
        assertThat(exactlyOneSuccess).isTrue();
    }

    @Test
    @DisplayName("Integration: Atomic Release - Only lock owner can release the lock")
    void testOwnerRestrictedRelease() {
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID impostorId = UUID.randomUUID();

        seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(ownerId)
                .ttlSeconds(300)
                .build());

        // Impostor attempts release
        boolean impostorRelease = seatLockService.releaseLock(showId, seatId, impostorId);
        assertThat(impostorRelease).isFalse();
        assertThat(seatLockService.isSeatLocked(showId, seatId)).isTrue();

        // Owner attempts release
        boolean ownerRelease = seatLockService.releaseLock(showId, seatId, ownerId);
        assertThat(ownerRelease).isTrue();
        assertThat(seatLockService.isSeatLocked(showId, seatId)).isFalse();
    }

    @Test
    @DisplayName("Integration: Renew Lock - Lock owner can extend TTL successfully")
    void testRenewLock_Success() {
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(ownerId)
                .ttlSeconds(60)
                .build());

        boolean renewed = seatLockService.renewLock(showId, seatId, ownerId, 600);
        assertThat(renewed).isTrue();

        String redisKey = "seat:" + showId + ":" + seatId;
        Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        assertThat(ttl).isGreaterThan(500L);
    }
}
