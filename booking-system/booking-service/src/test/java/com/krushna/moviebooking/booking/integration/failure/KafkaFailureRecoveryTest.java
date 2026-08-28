package com.krushna.moviebooking.booking.integration.failure;

import com.krushna.moviebooking.booking.outbox.OutboxEvent;
import com.krushna.moviebooking.booking.outbox.OutboxEventRepository;
import com.krushna.moviebooking.booking.outbox.OutboxEventService;
import com.krushna.moviebooking.booking.outbox.OutboxRelayScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 13 — Test 5: Kafka Failure / Recovery
 *
 * <p>Verifies the Transactional Outbox pattern holds under Kafka unavailability:
 * 1. When Kafka is inaccessible, outbox events remain PENDING / transition to FAILED after retries.
 * 2. When Kafka recovers, the OutboxRelayScheduler picks up PENDING/FAILED events and publishes them.
 * 3. No events are silently lost.
 *
 * <p>The Docker-level test (docker compose stop kafka) is annotated @Disabled and must be run via
 * the milestone-13-verify.ps1 script. The Testcontainer-based tests validate the relay logic.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("M13-Failure: Kafka failure — outbox relay preserves events, retries on recovery")
class KafkaFailureRecoveryTest {

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

    @Container
    static KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxRelayScheduler outboxRelayScheduler;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void clearOutbox() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Outbox event saved as PENDING — relay publishes to Kafka and marks PUBLISHED")
    void outboxEventSaved_relayPublishesAndMarksPublished() throws InterruptedException {
        String bookingRef = "BK-KAFKA-TEST-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> payload = Map.of(
                "bookingReference", bookingRef,
                "status", "CONFIRMED",
                "userId", UUID.randomUUID().toString()
        );

        // Save event — will be in PENDING status
        outboxEventService.saveEvent("Booking", bookingRef, "BOOKING_CONFIRMED", 1, payload);

        List<OutboxEvent> beforeRelay = outboxEventRepository.findAll();
        assertThat(beforeRelay).hasSize(1);
        assertThat(beforeRelay.get(0).getStatus()).isEqualTo("PENDING");

        System.out.println("===== M13 KAFKA RECOVERY EVIDENCE =====");
        System.out.println("Event saved → status: " + beforeRelay.get(0).getStatus());

        // Trigger relay manually (simulates scheduler firing after Kafka recovers)
        outboxRelayScheduler.processOutboxEvents();

        // Allow Kafka async callback
        Thread.sleep(2000);

        List<OutboxEvent> afterRelay = outboxEventRepository.findAll();
        String finalStatus = afterRelay.isEmpty() ? "UNKNOWN" : afterRelay.get(0).getStatus();

        System.out.println("After relay → status: " + finalStatus);
        System.out.println("=======================================");

        assertThat(afterRelay).hasSize(1);
        assertThat(finalStatus)
                .as("After successful relay, outbox event must transition to PUBLISHED")
                .isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("Relay with invalid topic: event transitions to FAILED after unknown event type")
    void outboxEventWithUnknownType_markedFailed() throws InterruptedException {
        String bookingRef = "BK-UNKNOWN-" + UUID.randomUUID().toString().substring(0, 8);

        // Manually persist an event with an unknown type to simulate corrupt data
        OutboxEvent badEvent = OutboxEvent.builder()
                .aggregateType("Booking")
                .aggregateId(bookingRef)
                .eventType("UNKNOWN_EVENT_TYPE")
                .payload("{\"ref\":\"" + bookingRef + "\"}")
                .status("PENDING")
                .retryCount(0)
                .build();
        outboxEventRepository.save(badEvent);

        outboxRelayScheduler.processOutboxEvents();
        Thread.sleep(500);

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        // Unknown event type → relay marks as FAILED, does not silently discard
        assertThat(events.get(0).getStatus())
                .as("Unknown event type must be marked FAILED, not silently dropped")
                .isEqualTo("FAILED");
    }

    @Test
    @DisplayName("No events pending — relay is a no-op and does not throw")
    void noEventsPending_relayIsNoOp() {
        // Ensure empty outbox
        outboxEventRepository.deleteAll();

        // Should not throw
        outboxRelayScheduler.processOutboxEvents();
        System.out.println("[KafkaFailureRecovery] Empty outbox relay: OK (no-op, no exception)");
    }

    // -------------------------------------------------------------------------
    // Docker-level Kafka failure test (requires running Docker stack)
    // -------------------------------------------------------------------------

    @Test
    @Disabled("Run via milestone-13-verify.ps1 with live Docker stack: docker compose stop kafka, create booking, verify PENDING, docker compose start kafka, verify PUBLISHED")
    @DisplayName("[MANUAL] Docker: Stop Kafka → booking outbox stays PENDING → restart Kafka → relay publishes")
    void dockerKafkaStopStart_outboxRecovers() {
        // This test is documented and executed via milestone-13-verify.ps1
        // Evidence captured in milestone-13-production-failure-verification.md
    }
}
