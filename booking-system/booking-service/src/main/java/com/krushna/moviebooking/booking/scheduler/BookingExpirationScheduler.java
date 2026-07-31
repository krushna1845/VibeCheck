package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled background task that periodically scans for expired pending booking reservations
 * and delegates single-booking cleanup to {@link ExpiredBookingProcessor}.
 *
 * <p>Executes every 60 seconds by default.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private static final List<String> EXPIRABLE_STATUSES = List.of("PENDING", "CREATED", "SEATS_LOCKED", "PAYMENT_PENDING");

    private final BookingRepository bookingRepository;
    private final ExpiredBookingProcessor expiredBookingProcessor;
    private final MeterRegistry meterRegistry;

    /**
     * Executes every 60 seconds to sweep abandoned seat reservations past their expiration timestamp.
     */
    @Scheduled(fixedRateString = "${booking.scheduler.expiration-rate-ms:60000}")
    public void sweepExpiredBookings() {
        Instant now = Instant.now();
        log.debug("Sweeping expired active bookings at timestamp: {}", now);

        Timer.Sample sample = Timer.start(meterRegistry);

        Counter.builder("booking.expiration.sweep.count")
                .description("Count of booking expiration sweep executions")
                .register(meterRegistry)
                .increment();

        List<Booking> expiredBookings = bookingRepository.findExpiredBookingsByStatuses(EXPIRABLE_STATUSES, now);
        if (expiredBookings.isEmpty()) {
            log.debug("No expired bookings found during sweep.");
            sample.stop(Timer.builder("booking.expiration.sweep.duration")
                    .description("Time taken to complete expired bookings sweep")
                    .register(meterRegistry));
            return;
        }

        log.info("Found {} expired pending/active bookings to process during sweep", expiredBookings.size());
        for (Booking booking : expiredBookings) {
            try {
                expiredBookingProcessor.processExpiredBooking(booking.getId());
            } catch (Exception e) {
                log.error("Failed to process expiration for booking ID: {}", booking.getId(), e);
            }
        }

        sample.stop(Timer.builder("booking.expiration.sweep.duration")
                .description("Time taken to complete expired bookings sweep")
                .register(meterRegistry));
    }
}
