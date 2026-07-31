package com.krushna.moviebooking.booking.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(processedEventRepository);
    }

    @Test
    @DisplayName("isEventProcessed should return true if eventId exists in repository")
    void testIsEventProcessedTrue() {
        when(processedEventRepository.existsById("EVT-101")).thenReturn(true);

        boolean result = idempotencyService.isEventProcessed("EVT-101");

        assertThat(result).isTrue();
        verify(processedEventRepository).existsById("EVT-101");
    }

    @Test
    @DisplayName("isEventProcessed should return false if eventId does not exist")
    void testIsEventProcessedFalse() {
        when(processedEventRepository.existsById("EVT-102")).thenReturn(false);

        boolean result = idempotencyService.isEventProcessed("EVT-102");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isEventProcessed should return false for blank or null eventId")
    void testIsEventProcessedNullOrBlank() {
        assertThat(idempotencyService.isEventProcessed(null)).isFalse();
        assertThat(idempotencyService.isEventProcessed("")).isFalse();
        verify(processedEventRepository, never()).existsById(anyString());
    }

    @Test
    @DisplayName("markEventAsProcessed should save ProcessedEvent record")
    void testMarkEventAsProcessed() {
        idempotencyService.markEventAsProcessed("EVT-103", "BOOKING_CREATED", "booking-group");

        ArgumentCaptor<ProcessedEvent> captor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(captor.capture());

        ProcessedEvent saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo("EVT-103");
        assertThat(saved.getEventType()).isEqualTo("BOOKING_CREATED");
        assertThat(saved.getConsumerGroup()).isEqualTo("booking-group");
    }
}
