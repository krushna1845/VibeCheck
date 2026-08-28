package com.krushna.moviebooking.booking.integration.concurrency;

import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.service.SeatLockService;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 13 — Test 1: Concurrency Test — Same Seat
 *
 * <p>Verifies that when 10 concurrent users attempt to acquire a Redis seat lock for the exact
 * same show seat, only ONE user succeeds (atomic SETNX) and the remaining 9 receive a
 * lock-failure response. No duplicate successful lock is created. Redis state is consistent.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("M13-Concurrency: 10 users racing for the same seat — exactly 1 winner")
class ConcurrentSeatBookingTest {

    private static final int CONCURRENT_USERS = 10;

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final UUID showId = UUID.randomUUID();
    private final UUID seatId = UUID.randomUUID();

    @AfterEach
    void cleanup() {
        Set<String> keys = stringRedisTemplate.keys("seat:" + showId + ":*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("10 concurrent users: exactly 1 booking wins the seat lock, 9 receive 409-equivalent failure")
    void tenConcurrentUsers_exactlyOneWinsTheSeatLock() throws InterruptedException, ExecutionException, TimeoutException {
        List<UUID> userIds = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            userIds.add(UUID.randomUUID());
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CyclicBarrier startGate = new CyclicBarrier(CONCURRENT_USERS);
        List<Future<SeatLockResponse>> futures = new ArrayList<>();

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final UUID userId = userIds.get(i);
            final int userIndex = i;
            futures.add(executor.submit(() -> {
                startGate.await();
                return seatLockService.lockSeats(SeatLockRequest.builder()
                        .showId(showId)
                        .seatIds(List.of(seatId))
                        .userId(userId)
                        .bookingReference("BK-RACE-" + userIndex)
                        .ttlSeconds(300L)
                        .build());
            }));
        }

        List<SeatLockResponse> responses = new ArrayList<>();
        for (Future<SeatLockResponse> f : futures) {
            responses.add(f.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long successCount = responses.stream().filter(SeatLockResponse::success).count();
        long failureCount = responses.stream().filter(r -> !r.success()).count();

        System.out.println("===== M13 CONCURRENCY EVIDENCE =====");
        System.out.println("Total requests:   " + CONCURRENT_USERS);
        System.out.println("Successful locks: " + successCount);
        System.out.println("Failed locks:     " + failureCount);

        String lockKey = "seat:" + showId + ":" + seatId;
        Boolean exists = stringRedisTemplate.hasKey(lockKey);
        Long ttl = stringRedisTemplate.getExpire(lockKey);
        System.out.println("Redis key exists: " + exists);
        System.out.println("Redis TTL (s):    " + ttl);
        System.out.println("=====================================");

        assertThat(successCount)
                .as("Exactly ONE user must win the seat lock")
                .isEqualTo(1L);

        assertThat(failureCount)
                .as("Remaining " + (CONCURRENT_USERS - 1) + " users must fail")
                .isEqualTo((long) (CONCURRENT_USERS - 1));

        assertThat(exists)
                .as("Redis must contain exactly one lock key")
                .isTrue();

        assertThat(ttl).isGreaterThan(0L);

        List<UUID> allLockedSeats = responses.stream()
                .filter(SeatLockResponse::success)
                .flatMap(r -> r.lockedSeatIds().stream())
                .collect(Collectors.toList());

        assertThat(allLockedSeats)
                .hasSize(1)
                .containsExactly(seatId);
    }

    @Test
    @DisplayName("After the winner releases the lock, a subsequent user can acquire it")
    void afterWinnerReleasesLock_nextUserCanAcquire() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        SeatLockResponse r1 = seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId).seatIds(List.of(seatId)).userId(user1).ttlSeconds(300L).build());
        assertThat(r1.success()).isTrue();

        SeatLockResponse r2 = seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId).seatIds(List.of(seatId)).userId(user2).ttlSeconds(300L).build());
        assertThat(r2.success()).isFalse();

        seatLockService.releaseLocks(showId, List.of(seatId));

        SeatLockResponse r3 = seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId).seatIds(List.of(seatId)).userId(user2).ttlSeconds(300L).build());
        assertThat(r3.success()).as("User 2 must acquire lock after User 1 releases").isTrue();
    }

    @Test
    @DisplayName("Batch atomicity: if one seat in a batch is taken, no seat in the batch is locked (rollback)")
    void batchAtomicity_ifOneSeatTakenNoneOfBatchLocked() {
        UUID seatA = UUID.randomUUID();
        UUID seatB = UUID.randomUUID();
        UUID ownerUser = UUID.randomUUID();
        UUID blockerUser = UUID.randomUUID();

        seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId).seatIds(List.of(seatB)).userId(blockerUser).ttlSeconds(300L).build());

        SeatLockResponse batchResp = seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId).seatIds(List.of(seatA, seatB)).userId(ownerUser).ttlSeconds(300L).build());

        assertThat(batchResp.success()).isFalse();

        boolean seatALocked = seatLockService.isSeatLocked(showId, seatA);
        assertThat(seatALocked)
                .as("seatA must have been rolled back (not orphaned) after batch failure")
                .isFalse();

        System.out.println("[BatchAtomicity] seatA orphaned lock: " + seatALocked + " — expected false (rollback successful)");
    }
}
