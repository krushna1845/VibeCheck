package com.krushna.moviebooking.booking.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSeatAvailabilityPublisher")
class WebSocketSeatAvailabilityPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketSeatAvailabilityPublisher publisher;

    private UUID showId;
    private UUID bookingId;
    private UUID userId;
    private List<UUID> seatIds;

    @BeforeEach
    void setUp() {
        showId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        seatIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    @DisplayName("broadcasts SEAT_LOCKED event to /topic/show/{showId}")
    void publishSeatAvailabilityEvent_SeatLocked_BroadcastsToCorrectTopic() {
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.SEAT_LOCKED)
                .showId(showId)
                .showSeatIds(seatIds)
                .bookingId(bookingId)
                .userId(userId)
                .bookingReference("BK1234567890")
                .build();

        publisher.publishSeatAvailabilityEvent(event);

        ArgumentCaptor<SeatAvailabilityEvent> captor = ArgumentCaptor.forClass(SeatAvailabilityEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/show/" + showId), captor.capture());

        SeatAvailabilityEvent captured = captor.getValue();
        assertThat(captured.eventType()).isEqualTo(SeatAvailabilityEventType.SEAT_LOCKED);
        assertThat(captured.showId()).isEqualTo(showId);
        assertThat(captured.showSeatIds()).hasSize(2);
        assertThat(captured.bookingId()).isEqualTo(bookingId);
    }

    @Test
    @DisplayName("broadcasts SEAT_RELEASED event to /topic/show/{showId}")
    void publishSeatAvailabilityEvent_SeatReleased_BroadcastsToCorrectTopic() {
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.SEAT_RELEASED)
                .showId(showId)
                .showSeatIds(seatIds)
                .bookingId(bookingId)
                .userId(userId)
                .build();

        publisher.publishSeatAvailabilityEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/show/" + showId), eq(event));
    }

    @Test
    @DisplayName("broadcasts BOOKING_CONFIRMED event to /topic/show/{showId}")
    void publishSeatAvailabilityEvent_BookingConfirmed_BroadcastsToCorrectTopic() {
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.BOOKING_CONFIRMED)
                .showId(showId)
                .showSeatIds(seatIds)
                .bookingId(bookingId)
                .userId(userId)
                .build();

        publisher.publishSeatAvailabilityEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/show/" + showId), eq(event));
    }

    @Test
    @DisplayName("broadcasts BOOKING_CANCELLED event to /topic/show/{showId}")
    void publishSeatAvailabilityEvent_BookingCancelled_BroadcastsToCorrectTopic() {
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.BOOKING_CANCELLED)
                .showId(showId)
                .showSeatIds(seatIds)
                .bookingId(bookingId)
                .userId(userId)
                .build();

        publisher.publishSeatAvailabilityEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/show/" + showId), eq(event));
    }

    @Test
    @DisplayName("broadcasts BOOKING_EXPIRED event to /topic/show/{showId}")
    void publishSeatAvailabilityEvent_BookingExpired_BroadcastsToCorrectTopic() {
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.BOOKING_EXPIRED)
                .showId(showId)
                .showSeatIds(seatIds)
                .bookingId(bookingId)
                .userId(userId)
                .build();

        publisher.publishSeatAvailabilityEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/show/" + showId), eq(event));
    }

    @Test
    @DisplayName("does not throw when SimpMessagingTemplate raises exception")
    void publishSeatAvailabilityEvent_TemplateThrows_DoesNotPropagate() {
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.SEAT_LOCKED)
                .showId(showId)
                .showSeatIds(seatIds)
                .bookingId(bookingId)
                .userId(userId)
                .build();

        doThrow(new RuntimeException("Broker unavailable"))
                .when(messagingTemplate).convertAndSend(anyString(), any(SeatAvailabilityEvent.class));

        // Should NOT throw — publish errors are swallowed with a log
        publisher.publishSeatAvailabilityEvent(event);
    }

    @Test
    @DisplayName("silently ignores null event without throwing")
    void publishSeatAvailabilityEvent_NullEvent_DoesNothing() {
        publisher.publishSeatAvailabilityEvent(null);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("silently ignores event with null showId without throwing")
    void publishSeatAvailabilityEvent_NullShowId_DoesNothing() {
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.SEAT_LOCKED)
                .showId(null)
                .showSeatIds(seatIds)
                .build();

        publisher.publishSeatAvailabilityEvent(event);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("destination correctly uses showId in topic path")
    void publishSeatAvailabilityEvent_DestinationContainsShowId() {
        UUID specificShowId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        SeatAvailabilityEvent event = SeatAvailabilityEvent.builder()
                .eventType(SeatAvailabilityEventType.SEAT_LOCKED)
                .showId(specificShowId)
                .showSeatIds(seatIds)
                .bookingId(bookingId)
                .userId(userId)
                .build();

        publisher.publishSeatAvailabilityEvent(event);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/show/00000000-0000-0000-0000-000000000001"),
                any(SeatAvailabilityEvent.class));
    }
}
