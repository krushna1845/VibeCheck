package com.krushna.moviebooking.booking.event;

/**
 * Domain publisher interface for broadcasting real-time seat availability updates over WebSocket.
 */
public interface SeatAvailabilityPublisher {

    /**
     * Broadcasts a seat availability event to topic subscribers listening at {@code /topic/show/{showId}}.
     *
     * @param event Detailed {@link SeatAvailabilityEvent} payload
     */
    void publishSeatAvailabilityEvent(SeatAvailabilityEvent event);
}
