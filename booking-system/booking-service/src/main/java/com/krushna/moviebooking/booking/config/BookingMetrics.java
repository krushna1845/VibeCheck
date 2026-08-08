package com.krushna.moviebooking.booking.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BookingMetrics {

    private final MeterRegistry meterRegistry;
    
    // Booking Counters
    private final Counter bookingCreatedCounter;
    private final Counter bookingConfirmedCounter;
    private final Counter bookingCancelledCounter;
    private final Counter bookingExpiredCounter;
    private final Counter bookingFailedCounter;
    
    // Seat Counters
    private final Counter seatsLockedCounter;
    private final Counter seatsBookedCounter;
    private final Counter seatsReleasedCounter;
    
    // Payment Counters
    private final Counter paymentInitiatedCounter;
    private final Counter paymentCompletedCounter;
    private final Counter paymentFailedCounter;
    
    // Timers
    private final Timer bookingCreationTimer;
    private final Timer bookingConfirmationTimer;
    private final Timer seatLockTimer;
    private final Timer paymentProcessingTimer;

    public BookingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize Counters
        this.bookingCreatedCounter = Counter.builder("booking.created.total")
                .description("Total number of bookings created")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.bookingConfirmedCounter = Counter.builder("booking.confirmed.total")
                .description("Total number of bookings confirmed")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.bookingCancelledCounter = Counter.builder("booking.cancelled.total")
                .description("Total number of bookings cancelled")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.bookingExpiredCounter = Counter.builder("booking.expired.total")
                .description("Total number of bookings expired")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.bookingFailedCounter = Counter.builder("booking.failed.total")
                .description("Total number of failed bookings")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.seatsLockedCounter = Counter.builder("seats.locked.total")
                .description("Total number of seats locked")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.seatsBookedCounter = Counter.builder("seats.booked.total")
                .description("Total number of seats booked")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.seatsReleasedCounter = Counter.builder("seats.released.total")
                .description("Total number of seats released")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.paymentInitiatedCounter = Counter.builder("payment.initiated.total")
                .description("Total number of payments initiated")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.paymentCompletedCounter = Counter.builder("payment.completed.total")
                .description("Total number of payments completed")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.paymentFailedCounter = Counter.builder("payment.failed.total")
                .description("Total number of payments failed")
                .tag("service", "booking-service")
                .register(meterRegistry);
        
        // Initialize Timers
        this.bookingCreationTimer = Timer.builder("booking.creation.duration")
                .description("Time taken to create a booking")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.bookingConfirmationTimer = Timer.builder("booking.confirmation.duration")
                .description("Time taken to confirm a booking")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.seatLockTimer = Timer.builder("seat.lock.duration")
                .description("Time taken to lock seats")
                .tag("service", "booking-service")
                .register(meterRegistry);
                
        this.paymentProcessingTimer = Timer.builder("payment.processing.duration")
                .description("Time taken to process payment")
                .tag("service", "booking-service")
                .register(meterRegistry);
    }

    // Booking Metrics
    public void incrementBookingCreated() {
        bookingCreatedCounter.increment();
    }

    public void incrementBookingConfirmed() {
        bookingConfirmedCounter.increment();
    }

    public void incrementBookingCancelled() {
        bookingCancelledCounter.increment();
    }

    public void incrementBookingExpired() {
        bookingExpiredCounter.increment();
    }

    public void incrementBookingFailed() {
        bookingFailedCounter.increment();
    }

    // Seat Metrics
    public void incrementSeatsLocked(int count) {
        seatsLockedCounter.increment(count);
    }

    public void incrementSeatsBooked(int count) {
        seatsBookedCounter.increment(count);
    }

    public void incrementSeatsReleased(int count) {
        seatsReleasedCounter.increment(count);
    }

    // Payment Metrics
    public void incrementPaymentInitiated() {
        paymentInitiatedCounter.increment();
    }

    public void incrementPaymentCompleted() {
        paymentCompletedCounter.increment();
    }

    public void incrementPaymentFailed() {
        paymentFailedCounter.increment();
    }

    // Timer Methods
    public void recordBookingCreation(long duration, TimeUnit unit) {
        bookingCreationTimer.record(duration, unit);
    }

    public void recordBookingConfirmation(long duration, TimeUnit unit) {
        bookingConfirmationTimer.record(duration, unit);
    }

    public void recordSeatLock(long duration, TimeUnit unit) {
        seatLockTimer.record(duration, unit);
    }

    public void recordPaymentProcessing(long duration, TimeUnit unit) {
        paymentProcessingTimer.record(duration, unit);
    }

    // Timer Context Methods
    public Timer.Sample startBookingCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public Timer.Sample startBookingConfirmationTimer() {
        return Timer.start(meterRegistry);
    }

    public Timer.Sample startSeatLockTimer() {
        return Timer.start(meterRegistry);
    }

    public Timer.Sample startPaymentProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopBookingCreationTimer(Timer.Sample sample) {
        sample.stop(bookingCreationTimer);
    }

    public void stopBookingConfirmationTimer(Timer.Sample sample) {
        sample.stop(bookingConfirmationTimer);
    }

    public void stopSeatLockTimer(Timer.Sample sample) {
        sample.stop(seatLockTimer);
    }

    public void stopPaymentProcessingTimer(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }
}
