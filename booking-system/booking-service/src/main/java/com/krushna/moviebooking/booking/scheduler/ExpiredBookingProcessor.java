package com.krushna.moviebooking.booking.scheduler;

import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.entity.BookingSeat;
import com.krushna.moviebooking.booking.event.BookingEventPublisher;
import com.krushna.moviebooking.booking.event.BookingExpiredEvent;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.service.SeatLockService;
import com.krushna.moviebooking.booking.statemachine.BookingStateMachine;
import com.krushna.moviebooking.booking.statemachine.BookingStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Single-booking expiration processor operating within an independent transaction with retry support.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Releases temporary Redis seat locks</li>
 *   <li>Transitions booking status to EXPIRED via state machine</li>
 *   <li>Publishes BookingExpiredEvent to Kafka</li>
 *   <li>Records Micrometer telemetry metrics</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredBookingProcessor {

    private final BookingRepository bookingRepository;
    private final SeatLockService seatLockService;
    private final BookingEventPublisher bookingEventPublisher;
    private final BookingStateMachine bookingStateMachine;
    private final MeterRegistry meterRegistry;

    /**
     * Processes expiration for a single booking in an independent transaction with automated retries.
     *
     * @param bookingId UUID of the target booking
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void processExpiredBooking(UUID bookingId) {
        log.info("Processing expiration for booking ID: {}", bookingId);

        Timer.Sample sample = Timer.start(meterRegistry);

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getDeletedAt() != null) {
            log.warn("Booking ID {} not found or has been soft-deleted. Skipping expiration.", bookingId);
            return;
        }

        BookingStatus currentStatus = bookingStateMachine.currentStatus(booking);
        if (currentStatus.isTerminal()) {
            log.info("Booking ID {} is already in terminal state {}. Skipping expiration.", bookingId, currentStatus);
            return;
        }

        List<UUID> showSeatIds = booking.getBookingSeats().stream()
                .map(BookingSeat::getShowSeatId)
                .toList();

        // 1. Release Redis seat locks
        if (!showSeatIds.isEmpty()) {
            log.debug("Releasing Redis seat locks for showId: {}, seatCount: {}", booking.getShowId(), showSeatIds.size());
            seatLockService.releaseLocks(booking.getShowId(), showSeatIds);
        }

        // 2. Transition status to EXPIRED using state machine
        bookingStateMachine.transition(booking, BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        // 3. Publish BookingExpiredEvent
        BookingExpiredEvent expiredEvent = BookingExpiredEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .showSeatIds(showSeatIds)
                .timestamp(Instant.now())
                .build();
        bookingEventPublisher.publishBookingExpired(expiredEvent);

        // 4. Record Metrics
        Counter.builder("booking.expiration.success")
                .description("Count of successfully expired bookings")
                .register(meterRegistry)
                .increment();

        sample.stop(Timer.builder("booking.expiration.duration")
                .description("Time spent processing single booking expiration")
                .register(meterRegistry));

        log.info("Successfully expired booking ID: {}, reference: {}", bookingId, booking.getBookingReference());
    }

    /**
     * Fallback recovery handler called when retries for processing an expired booking are exhausted.
     *
     * @param e Exception that caused the retry failure
     * @param bookingId Target booking UUID
     */
    @Recover
    public void recoverExpiredBooking(Exception e, UUID bookingId) {
        log.error("Exhausted retries attempting to expire booking ID: {}. Error: {}", bookingId, e.getMessage(), e);

        Counter.builder("booking.expiration.failure")
                .description("Count of failed booking expirations after retries")
                .register(meterRegistry)
                .increment();
    }
}
