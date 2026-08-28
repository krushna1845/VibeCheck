package com.krushna.moviebooking.booking.integration.idempotency;

import com.krushna.moviebooking.booking.idempotency.IdempotencyService;
import com.krushna.moviebooking.booking.idempotency.ProcessedEvent;
import com.krushna.moviebooking.booking.idempotency.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Milestone 13 — Test 2: Duplicate Booking Idempotency
 *
 * <p>Verifies that the IdempotencyService correctly prevents duplicate Kafka event processing.
 * Same eventId submitted 4 times → marked processed only once → subsequent calls return true
 * for isEventProcessed without writing additional rows.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("M13-Idempotency: Booking event idempotency — duplicate eventId processed only once")
class BookingIdempotencyTest {

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

    @BeforeEach
    void clearTable() {
        processedEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Request 1 → creates record; Requests 2-4 → duplicate detected, no new record")
    void sameEventId_processedOnlyOnce() {
        String eventId = "evt-" + UUID.randomUUID();

        // Request 1 — not yet processed
        assertThat(idempotencyService.isEventProcessed(eventId))
                .as("Before marking, isEventProcessed must be false")
                .isFalse();

        idempotencyService.markEventAsProcessed(eventId, "BOOKING_CREATED", "booking-service-group");

        // Requests 2, 3, 4 — duplicates
        for (int i = 2; i <= 4; i++) {
            boolean isDuplicate = idempotencyService.isEventProcessed(eventId);
            assertThat(isDuplicate)
                    .as("Request " + i + " must be recognised as a duplicate")
                    .isTrue();
        }

        // Database state: exactly ONE record in processed_events
        long count = processedEventRepository.count();
        assertThat(count)
                .as("Exactly one processed_events row must exist for this eventId")
                .isEqualTo(1L);

        System.out.println("===== M13 IDEMPOTENCY (Booking) EVIDENCE =====");
        System.out.println("eventId: " + eventId);
        System.out.println("processed_events rows: " + count);
        System.out.println("===============================================");
    }

    @Test
    @DisplayName("Two distinct eventIds create two separate records independently")
    void distinctEventIds_eachRecordedOnce() {
        String eventId1 = "evt-A-" + UUID.randomUUID();
        String eventId2 = "evt-B-" + UUID.randomUUID();

        idempotencyService.markEventAsProcessed(eventId1, "BOOKING_CREATED", "booking-service-group");
        idempotencyService.markEventAsProcessed(eventId2, "BOOKING_CONFIRMED", "booking-service-group");

        assertThat(idempotencyService.isEventProcessed(eventId1)).isTrue();
        assertThat(idempotencyService.isEventProcessed(eventId2)).isTrue();
        assertThat(processedEventRepository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Delayed duplicate: marking processed, then checking again after a delay still returns duplicate")
    void delayedDuplicate_stillDetected() throws InterruptedException {
        String eventId = "evt-delayed-" + UUID.randomUUID();

        idempotencyService.markEventAsProcessed(eventId, "BOOKING_CONFIRMED", "group-1");
        assertThat(idempotencyService.isEventProcessed(eventId)).isTrue();

        // Simulate delay (Kafka re-delivery scenario)
        Thread.sleep(100);

        // Re-delivery attempt — must still be detected as duplicate
        assertThat(idempotencyService.isEventProcessed(eventId))
                .as("Delayed duplicate re-delivery must still be detected")
                .isTrue();

        assertThat(processedEventRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Null or blank eventId is safely ignored — no record created")
    void nullOrBlankEventId_safelyIgnored() {
        assertThat(idempotencyService.isEventProcessed(null)).isFalse();
        assertThat(idempotencyService.isEventProcessed("")).isFalse();
        assertThat(idempotencyService.isEventProcessed("   ")).isFalse();

        idempotencyService.markEventAsProcessed(null, "BOOKING_CREATED", "group");
        idempotencyService.markEventAsProcessed("", "BOOKING_CREATED", "group");

        assertThat(processedEventRepository.count()).isEqualTo(0L);
    }
}
