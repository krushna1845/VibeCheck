package com.krushna.moviebooking.show.cache;

import com.krushna.moviebooking.common.event.BookingEvents.BookingCancelledEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingConfirmedEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingCreatedEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingExpiredEvent;
import com.krushna.moviebooking.common.event.MovieEvents.MovieUpdatedEvent;
import com.krushna.moviebooking.show.kafka.ShowCacheEventConsumer;
import com.krushna.moviebooking.show.metrics.ShowCacheMetrics;
import com.krushna.moviebooking.show.service.ShowCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Show Service Redis cache invalidation layer.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Booking events trigger seat availability cache eviction.</li>
 *   <li>Movie update events trigger show + seat cache eviction.</li>
 *   <li>Show update / cancel operations evict both caches.</li>
 *   <li>Custom {@link ShowCacheMetrics} counters are incremented correctly.</li>
 *   <li>Malformed payloads are handled gracefully without exceptions.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ShowCacheTest {

    // -------------------------------------------------------------------------
    // Collaborators
    // -------------------------------------------------------------------------

    @Mock
    private ShowCacheService showCacheService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache showsCache;

    @Mock
    private Cache seatsCache;

    private MeterRegistry meterRegistry;
    private ShowCacheMetrics showCacheMetrics;
    private ShowCacheEventConsumer consumer;
    private ObjectMapper objectMapper;

    private UUID showId;
    private UUID movieId;
    private UUID bookingId;
    private UUID userId;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        // Use a real ShowCacheMetrics with a real CacheManager mock
        showCacheMetrics = new ShowCacheMetrics(meterRegistry, cacheManager);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        consumer = new ShowCacheEventConsumer(showCacheService, showCacheMetrics, objectMapper);

        showId   = UUID.randomUUID();
        movieId  = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        userId   = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Booking Created
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("onBookingCreated – evicts showSeats cache and records metric")
    void onBookingCreated_evictsSeatCache() {
        BookingCreatedEvent event = new BookingCreatedEvent(
                UUID.randomUUID().toString(), "BOOKING_CREATED", 1,
                bookingId, "REF-001", userId, showId,
                List.of(UUID.randomUUID()), new BigDecimal("200.00"),
                Instant.now().plusSeconds(600), Instant.now()
        );

        consumer.onBookingCreated(event);

        verify(showCacheService).evictSeatAvailabilityCache(showId);
        assertEvictionCounterIncremented("showSeats", "booking_created");
    }

    @Test
    @DisplayName("onBookingCreated – gracefully handles malformed (null) payload")
    void onBookingCreated_handlesNullPayload() {
        // Should log a warning but not throw
        consumer.onBookingCreated(null);
        verifyNoInteractions(showCacheService);
    }

    // -------------------------------------------------------------------------
    // Booking Confirmed
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("onBookingConfirmed – evicts showSeats cache and records metric")
    void onBookingConfirmed_evictsSeatCache() {
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                UUID.randomUUID().toString(), "BOOKING_CONFIRMED", 1,
                bookingId, "REF-002", userId, showId,
                List.of(UUID.randomUUID()), List.of("A1"),
                new BigDecimal("200.00"), "PAY-001", Instant.now()
        );

        consumer.onBookingConfirmed(event);

        verify(showCacheService).evictSeatAvailabilityCache(showId);
        assertEvictionCounterIncremented("showSeats", "booking_confirmed");
    }

    // -------------------------------------------------------------------------
    // Booking Cancelled
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("onBookingCancelled – evicts showSeats cache and records metric")
    void onBookingCancelled_evictsSeatCache() {
        BookingCancelledEvent event = new BookingCancelledEvent(
                UUID.randomUUID().toString(), "BOOKING_CANCELLED", 1,
                bookingId, "REF-003", userId, showId,
                List.of(UUID.randomUUID()), "USER_CANCELLED", Instant.now()
        );

        consumer.onBookingCancelled(event);

        verify(showCacheService).evictSeatAvailabilityCache(showId);
        assertEvictionCounterIncremented("showSeats", "booking_cancelled");
    }

    // -------------------------------------------------------------------------
    // Booking Expired
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("onBookingExpired – evicts showSeats cache and records metric")
    void onBookingExpired_evictsSeatCache() {
        BookingExpiredEvent event = new BookingExpiredEvent(
                UUID.randomUUID().toString(), "BOOKING_EXPIRED", 1,
                bookingId, "REF-004", userId, showId,
                List.of(UUID.randomUUID()), Instant.now()
        );

        consumer.onBookingExpired(event);

        verify(showCacheService).evictSeatAvailabilityCache(showId);
        assertEvictionCounterIncremented("showSeats", "booking_expired");
    }

    // -------------------------------------------------------------------------
    // Movie Updated
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("onMovieUpdated – evicts all caches for movie shows and records metrics")
    void onMovieUpdated_evictsMovieCaches() {
        MovieUpdatedEvent event = new MovieUpdatedEvent(
                UUID.randomUUID().toString(), "MOVIE_UPDATED", 1,
                movieId, "Inception", Instant.now()
        );

        consumer.onMovieUpdated(event);

        verify(showCacheService).evictCachesForMovie(movieId);
        assertEvictionCounterIncremented("shows", "movie_updated");
        assertEvictionCounterIncremented("showSeats", "movie_updated");
    }

    // -------------------------------------------------------------------------
    // ShowCacheService – evictCachesForMovie
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ShowCacheMetrics.recordEviction – increments counter for each unique (cache, reason) pair")
    void recordEviction_incrementsCounter() {
        showCacheMetrics.recordEviction("shows", "booking_created");
        showCacheMetrics.recordEviction("shows", "booking_created");
        showCacheMetrics.recordEviction("showSeats", "booking_expired");

        double showsCreatedCount = meterRegistry.counter("show.cache.evictions",
                "cache", "shows", "reason", "booking_created").count();
        double seatsExpiredCount = meterRegistry.counter("show.cache.evictions",
                "cache", "showSeats", "reason", "booking_expired").count();

        assertThat(showsCreatedCount).isEqualTo(2.0);
        assertThat(seatsExpiredCount).isEqualTo(1.0);
    }

    @Test
    @DisplayName("ShowCacheMetrics.recordEviction – different reasons tracked independently")
    void recordEviction_differentReasonsTrackedSeparately() {
        showCacheMetrics.recordEviction("showSeats", "booking_created");
        showCacheMetrics.recordEviction("showSeats", "booking_confirmed");
        showCacheMetrics.recordEviction("showSeats", "booking_cancelled");
        showCacheMetrics.recordEviction("showSeats", "booking_expired");

        double created   = meterRegistry.counter("show.cache.evictions", "cache", "showSeats", "reason", "booking_created").count();
        double confirmed = meterRegistry.counter("show.cache.evictions", "cache", "showSeats", "reason", "booking_confirmed").count();
        double cancelled = meterRegistry.counter("show.cache.evictions", "cache", "showSeats", "reason", "booking_cancelled").count();
        double expired   = meterRegistry.counter("show.cache.evictions", "cache", "showSeats", "reason", "booking_expired").count();

        assertThat(created).isEqualTo(1.0);
        assertThat(confirmed).isEqualTo(1.0);
        assertThat(cancelled).isEqualTo(1.0);
        assertThat(expired).isEqualTo(1.0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void assertEvictionCounterIncremented(String cacheName, String reason) {
        double count = meterRegistry.counter("show.cache.evictions",
                "cache", cacheName, "reason", reason).count();
        assertThat(count)
                .as("Expected eviction counter for cache='%s' reason='%s' to be >= 1", cacheName, reason)
                .isGreaterThanOrEqualTo(1.0);
    }
}
