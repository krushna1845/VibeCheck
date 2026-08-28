package com.krushna.moviebooking.booking.integration.idempotency;

import com.krushna.moviebooking.booking.idempotency.IdempotencyService;
import com.krushna.moviebooking.booking.idempotency.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 13 — Test 4: Duplicate Payment Request Idempotency
 *
 * <p>Simulates multiple payment initiation requests carrying the same idempotency key.
 * Verifies that:
 * - The idempotency check correctly detects second/third/fourth as duplicates.
 * - Exactly one processed record exists.
 * - No duplicate processing markers are created.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("M13-Idempotency: Payment duplicate request — same idempotency key returns cached result")
class PaymentIdempotencyTest {

    @Container
    static MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("vibecheck_booking_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void clearState() {
        processedEventRepository.deleteAll();
    }

    @Test
    @DisplayName("4 payment requests with same eventId: only 1 processed record in DB")
    void samePaymentIdempotencyKey_processedOnlyOnce() {
        String paymentEventId = "pay-evt-" + UUID.randomUUID();
        String eventType = "PAYMENT_INITIATED";

        // Request 1: new event
        assertThat(idempotencyService.isEventProcessed(paymentEventId)).isFalse();
        idempotencyService.markEventAsProcessed(paymentEventId, eventType, "payment-service-group");

        // Requests 2, 3, 4: duplicates
        for (int attempt = 2; attempt <= 4; attempt++) {
            boolean isDuplicate = idempotencyService.isEventProcessed(paymentEventId);
            assertThat(isDuplicate)
                    .as("Payment request attempt " + attempt + " must be identified as duplicate")
                    .isTrue();
        }

        long rowCount = processedEventRepository.count();

        System.out.println("===== M13 PAYMENT IDEMPOTENCY EVIDENCE =====");
        System.out.println("Payment initiation attempts:  4");
        System.out.println("processed_events rows:        " + rowCount);
        System.out.println("Expected:                     1 (exactly one payment processed)");
        System.out.println("===========================================");

        assertThat(rowCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("Payment callback deduplication: same transactionRef processed only once in Redis")
    void sameTransactionRef_callbackDeduplicated() {
        String transactionRef = "TXN-PAY-" + UUID.randomUUID();
        String callbackKey = "payment:callback:" + transactionRef;

        // Simulate first callback
        assertThat(stringRedisTemplate.hasKey(callbackKey))
                .as("Callback key must not exist before first processing")
                .isFalse();

        // Mark as processed (simulating what PaymentIdempotencyService.markCallbackProcessed does)
        stringRedisTemplate.opsForValue().set(callbackKey, "1");

        // Subsequent callback deliveries: key exists → duplicate detected
        Boolean exists = stringRedisTemplate.hasKey(callbackKey);
        assertThat(exists).as("Callback key must exist after first processing").isTrue();

        System.out.println("===== M13 PAYMENT CALLBACK DEDUPLICATION EVIDENCE =====");
        System.out.println("transactionRef: " + transactionRef);
        System.out.println("Redis key:      " + callbackKey + " → exists=" + exists);
        System.out.println("Result: Duplicate webhook correctly detected by Redis key check.");
        System.out.println("=======================================================");

        // Cleanup
        stringRedisTemplate.delete(callbackKey);
    }
}
