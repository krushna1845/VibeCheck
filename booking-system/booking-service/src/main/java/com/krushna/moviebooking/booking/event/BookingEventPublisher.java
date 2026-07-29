package com.krushna.moviebooking.booking.event;

/**
 * Event publisher interface for emitting booking lifecycle domain events.
 */
public interface BookingEventPublisher {

    void publishBookingCreated(BookingCreatedEvent event);

    void publishBookingConfirmed(BookingConfirmedEvent event);

    void publishBookingCancelled(BookingCancelledEvent event);

    void publishBookingExpired(BookingExpiredEvent event);

    void publishBookingFailed(BookingFailedEvent event);
}
