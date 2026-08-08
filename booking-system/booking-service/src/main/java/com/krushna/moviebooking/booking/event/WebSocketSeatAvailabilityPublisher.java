package com.krushna.moviebooking.booking.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Spring Component implementation of {@link SeatAvailabilityPublisher} using {@link SimpMessagingTemplate}
 * to broadcast real-time seat availability updates to STOMP WebSocket topic {@code /topic/show/{showId}}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSeatAvailabilityPublisher implements SeatAvailabilityPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publishSeatAvailabilityEvent(SeatAvailabilityEvent event) {
        if (event == null || event.showId() == null) {
            log.warn("[WebSocketPublisher] Cannot publish null event or event missing showId");
            return;
        }

        String destination = "/topic/show/" + event.showId();
        log.info("[WebSocketPublisher] Broadcasting {} event for showId: {}, seatCount: {} to destination: {}",
                event.eventType(), event.showId(), event.showSeatIds() != null ? event.showSeatIds().size() : 0, destination);

        try {
            messagingTemplate.convertAndSend(destination, event);
            log.debug("[WebSocketPublisher] Broadcast sent successfully to {}", destination);
        } catch (Exception e) {
            log.error("[WebSocketPublisher] Error publishing event to STOMP destination {}: {}", destination, e.getMessage(), e);
        }
    }
}
