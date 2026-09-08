package com.krushna.moviebooking.booking.integration.lifecycle;

import com.krushna.moviebooking.booking.dto.BookingResponse;
import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.entity.BookingSeat;
import com.krushna.moviebooking.booking.outbox.OutboxEvent;
import com.krushna.moviebooking.booking.outbox.OutboxEventRepository;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.service.BookingService;
import com.krushna.moviebooking.booking.service.SeatLockService;
import com.krushna.moviebooking.booking.client.ShowClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 13 — Test 8: Payment Failure Test
 *
 * <p>Simulates:
 * Booking -> PENDING
 * Payment -> FAILED (Customer cancel / Payment Gateway rejection)
 * Booking -> CANCELLED
 * Redis Lock -> RELEASED
 * Outbox Event -> BOOKING_CANCELLED generated
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("M13-Lifecycle: Payment Failure — cancels booking and releases locks safely")
class PaymentFailureTest {

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
    private BookingRepository bookingRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private BookingService bookingService;

    @MockitoBean
    private ShowClient showClient;

    @BeforeEach
    void setup() {
        outboxEventRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    @DisplayName("Payment failure triggers cancelBooking, releases Redis locks, emits outbox event")
    void paymentFailed_cancelsBookingAndReleasesLocks() {
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String bookingRef = "BK-F-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 1. Lock seat in Redis
        SeatLockResponse lockResp = seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(userId)
                .bookingReference(bookingRef)
                .ttlSeconds(300L)
                .build());
        assertThat(lockResp.success()).isTrue();

        // 2. Persist PENDING booking
        Booking booking = Booking.builder()
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .status("PENDING")
                .totalAmount(new BigDecimal("300.00"))
                .taxAmount(new BigDecimal("54.00"))
                .convenienceFee(new BigDecimal("30.00"))
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .bookingSeats(new ArrayList<>())
                .build();

        BookingSeat seat = BookingSeat.builder()
                .booking(booking)
                .showSeatId(seatId)
                .seatNumber("B1")
                .price(new BigDecimal("300.00"))
                .build();
        booking.getBookingSeats().add(seat);
        bookingRepository.save(booking);

        System.out.println("===== M13 PAYMENT FAILURE EVIDENCE =====");
        System.out.println("Booking created: " + bookingRef + " Status: PENDING");
        System.out.println("Redis lock active: " + seatLockService.isSeatLocked(showId, seatId));

        // 3. Cancel booking due to payment failure
        BookingResponse cancelResp = bookingService.cancelBooking(bookingRef, "Payment failed at gateway");

        System.out.println("Post-cancel status: " + cancelResp.status());
        System.out.println("Redis lock active: " + seatLockService.isSeatLocked(showId, seatId));

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        System.out.println("Outbox events recorded: " + outboxEvents.size());
        outboxEvents.forEach(e -> System.out.println("  " + e.getEventType() + " -> " + e.getStatus()));
        System.out.println("=========================================");

        assertThat(cancelResp.status()).isEqualTo("CANCELLED");
        assertThat(seatLockService.isSeatLocked(showId, seatId)).isFalse();
        assertThat(outboxEvents).anyMatch(e -> "BOOKING_CANCELLED".equals(e.getEventType()));
    }
}
