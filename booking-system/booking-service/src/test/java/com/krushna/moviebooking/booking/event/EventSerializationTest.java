package com.krushna.moviebooking.booking.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should serialize and deserialize BookingCreatedEvent with version and metadata")
    void testBookingCreatedEventSerialization() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID showId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        Instant now = Instant.now();

        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(bookingId)
                .bookingReference("BKG-12345678")
                .userId(userId)
                .showId(showId)
                .showSeatIds(List.of(seatId))
                .totalAmount(new BigDecimal("250.00"))
                .expiresAt(now.plusSeconds(600))
                .timestamp(now)
                .build();

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains("BOOKING_CREATED");
        assertThat(json).contains("BKG-12345678");

        BookingCreatedEvent deserialized = objectMapper.readValue(json, BookingCreatedEvent.class);
        assertThat(deserialized.eventId()).isEqualTo(event.eventId());
        assertThat(deserialized.eventType()).isEqualTo("BOOKING_CREATED");
        assertThat(deserialized.eventVersion()).isEqualTo(1);
        assertThat(deserialized.bookingId()).isEqualTo(bookingId);
        assertThat(deserialized.bookingReference()).isEqualTo("BKG-12345678");
    }

    @Test
    @DisplayName("Should serialize and deserialize BookingConfirmedEvent")
    void testBookingConfirmedEventSerialization() throws Exception {
        UUID bookingId = UUID.randomUUID();
        BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                .bookingId(bookingId)
                .bookingReference("BKG-87654321")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .seatNumbers(List.of("A1"))
                .totalAmount(new BigDecimal("300.00"))
                .paymentId("PAY-999")
                .timestamp(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(event);
        BookingConfirmedEvent deserialized = objectMapper.readValue(json, BookingConfirmedEvent.class);

        assertThat(deserialized.eventType()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(deserialized.eventVersion()).isEqualTo(1);
        assertThat(deserialized.paymentId()).isEqualTo("PAY-999");
    }

    @Test
    @DisplayName("Should serialize and deserialize BookingCancelledEvent")
    void testBookingCancelledEventSerialization() throws Exception {
        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-CANCELLED")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .reason("User requested cancellation")
                .timestamp(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(event);
        BookingCancelledEvent deserialized = objectMapper.readValue(json, BookingCancelledEvent.class);

        assertThat(deserialized.eventType()).isEqualTo("BOOKING_CANCELLED");
        assertThat(deserialized.reason()).isEqualTo("User requested cancellation");
    }

    @Test
    @DisplayName("Should serialize and deserialize BookingExpiredEvent")
    void testBookingExpiredEventSerialization() throws Exception {
        BookingExpiredEvent event = BookingExpiredEvent.builder()
                .bookingId(UUID.randomUUID())
                .bookingReference("BKG-EXPIRED")
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .showSeatIds(List.of(UUID.randomUUID()))
                .timestamp(Instant.now())
                .build();

        String json = objectMapper.writeValueAsString(event);
        BookingExpiredEvent deserialized = objectMapper.readValue(json, BookingExpiredEvent.class);

        assertThat(deserialized.eventType()).isEqualTo("BOOKING_EXPIRED");
        assertThat(deserialized.bookingReference()).isEqualTo("BKG-EXPIRED");
    }
}
