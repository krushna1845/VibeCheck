package com.krushna.moviebooking.booking.integration.lifecycle;

import com.krushna.moviebooking.booking.dto.SeatLockRequest;
import com.krushna.moviebooking.booking.dto.SeatLockResponse;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.entity.BookingSeat;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.scheduler.BookingExpirationScheduler;
import com.krushna.moviebooking.booking.scheduler.ExpiredBookingProcessor;
import com.krushna.moviebooking.booking.service.SeatLockService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Milestone 13 — Test 7: Booking Expiration Test
 *
 * <p>Verifies:
 * 1. Booking in PENDING state with past expiration timestamp is swept by BookingExpirationScheduler.
 * 2. Booking transitions from PENDING -> EXPIRED.
 * 3. Redis seat lock is released.
 * 4. Seat becomes available again for new bookings.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("M13-Lifecycle: Booking Expiration — sweeps expired reservations and releases Redis locks")
class BookingExpirationTest {

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
    private SeatLockService seatLockService;

    @Autowired
    private BookingExpirationScheduler expirationScheduler;

    @Autowired
    private ExpiredBookingProcessor expiredBookingProcessor;

    @BeforeEach
    void setup() {
        bookingRepository.deleteAll();
    }

    @Test
    @DisplayName("Expired pending booking transitions to EXPIRED and releases seat locks")
    void expiredBooking_isCleanedUpAndLockReleased() {
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String bookingRef = "BK-EXP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // 1. Lock the seat in Redis
        SeatLockResponse lockResp = seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(userId)
                .bookingReference(bookingRef)
                .ttlSeconds(300L)
                .build());
        assertThat(lockResp.success()).isTrue();
        assertThat(seatLockService.isSeatLocked(showId, seatId)).isTrue();

        // 2. Persist booking that already expired 1 minute ago
        Booking booking = Booking.builder()
                .bookingReference(bookingRef)
                .userId(userId)
                .showId(showId)
                .status("PENDING")
                .totalAmount(new BigDecimal("250.00"))
                .taxAmount(new BigDecimal("45.00"))
                .convenienceFee(new BigDecimal("30.00"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .bookingSeats(new ArrayList<>())
                .build();

        BookingSeat seat = BookingSeat.builder()
                .booking(booking)
                .showSeatId(seatId)
                .seatNumber("A1")
                .price(new BigDecimal("250.00"))
                .build();
        booking.getBookingSeats().add(seat);

        Booking saved = bookingRepository.save(booking);

        System.out.println("===== M13 BOOKING EXPIRATION EVIDENCE =====");
        System.out.println("Booking created ID: " + saved.getId() + " Ref: " + bookingRef);
        System.out.println("Initial status: " + saved.getStatus());
        System.out.println("Initial Redis lock: " + seatLockService.isSeatLocked(showId, seatId));

        // 3. Trigger expiration scheduler sweep
        expirationScheduler.sweepExpiredBookings();

        // 4. Verify DB state is now EXPIRED
        Booking refreshed = bookingRepository.findById(saved.getId()).orElseThrow();
        System.out.println("Post-sweep status: " + refreshed.getStatus());
        System.out.println("Post-sweep Redis lock: " + seatLockService.isSeatLocked(showId, seatId));
        System.out.println("===========================================");

        assertThat(refreshed.getStatus()).isEqualTo("EXPIRED");
        assertThat(seatLockService.isSeatLocked(showId, seatId)).isFalse();

        // 5. Verify seat can now be locked by another user
        UUID nextUser = UUID.randomUUID();
        SeatLockResponse nextLock = seatLockService.lockSeats(SeatLockRequest.builder()
                .showId(showId)
                .seatIds(List.of(seatId))
                .userId(nextUser)
                .bookingReference("BK-NEXT-" + UUID.randomUUID().toString().substring(0, 4))
                .ttlSeconds(300L)
                .build());
        assertThat(nextLock.success()).isTrue();

        seatLockService.releaseLocks(showId, List.of(seatId));
    }
}
