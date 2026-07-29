package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled background task that periodically scans for expired pending booking reservations
 * and triggers expiration and Redis seat lock release.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    /**
     * Executes every 30 seconds to clean up abandoned seat reservations.
     */
    @Scheduled(cron = "${booking.expiration-cron:*/30 * * * * *}")
    public void sweepExpiredBookings() {
        Instant now = Instant.now();
        log.debug("Sweeping expired pending bookings at timestamp: {}", now);

        List<Booking> expiredBookings = bookingRepository.findExpiredBookings("PENDING", now);
        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired pending bookings to process", expiredBookings.size());
        for (Booking booking : expiredBookings) {
            try {
                bookingService.expireBooking(booking.getId());
            } catch (Exception e) {
                log.error("Failed to expire booking ID: {}", booking.getId(), e);
            }
        }
    }
}
