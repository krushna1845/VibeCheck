package com.krushna.moviebooking.show.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.common.event.BookingEvents.BookingCancelledEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingConfirmedEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingCreatedEvent;
import com.krushna.moviebooking.common.event.BookingEvents.BookingExpiredEvent;
import com.krushna.moviebooking.common.event.MovieEvents.MovieUpdatedEvent;
import com.krushna.moviebooking.show.config.ShowKafkaConfig;
import com.krushna.moviebooking.show.metrics.ShowCacheMetrics;
import com.krushna.moviebooking.show.service.ShowCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka event consumer that listens to booking and movie events to invalidate
 * Redis caches in the show-service.
 *
 * <h2>Cache invalidation triggers</h2>
 * <ul>
 *   <li><b>booking-created-events</b>  – evicts seat availability for the show.</li>
 *   <li><b>booking-confirmed-events</b> – evicts seat availability for the show.</li>
 *   <li><b>booking-cancelled-events</b> – evicts seat availability for the show.</li>
 *   <li><b>booking-expired-events</b>   – evicts seat availability for the show.</li>
 *   <li><b>movie-updated-events</b>     – evicts all show details and seats cached
 *       for shows belonging to the updated movie.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShowCacheEventConsumer {

    private final ShowCacheService showCacheService;
    private final ShowCacheMetrics showCacheMetrics;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // BOOKING EVENTS
    // -------------------------------------------------------------------------

    /**
     * Evicts the {@code showSeats} cache entry whenever a new booking is created
     * for the show (seats move from AVAILABLE → RESERVED).
     */
    @KafkaListener(
            topics = ShowKafkaConfig.BOOKING_CREATED_TOPIC,
            groupId = ShowKafkaConfig.CONSUMER_GROUP_ID,
            containerFactory = "showKafkaListenerContainerFactory"
    )
    public void onBookingCreated(@Payload Object payload) {
        try {
            BookingCreatedEvent event = convertPayload(payload, BookingCreatedEvent.class);
            log.info("[ShowCacheConsumer] BookingCreated – evicting seats cache for showId={}", event.showId());
            showCacheService.evictSeatAvailabilityCache(event.showId());
            showCacheMetrics.recordEviction("showSeats", "booking_created");
        } catch (Exception e) {
            log.warn("[ShowCacheConsumer] Failed to process BookingCreatedEvent for cache eviction", e);
        }
    }

    /**
     * Evicts the {@code showSeats} cache entry when a booking is confirmed
     * (seats move to BOOKED).
     */
    @KafkaListener(
            topics = ShowKafkaConfig.BOOKING_CONFIRMED_TOPIC,
            groupId = ShowKafkaConfig.CONSUMER_GROUP_ID,
            containerFactory = "showKafkaListenerContainerFactory"
    )
    public void onBookingConfirmed(@Payload Object payload) {
        try {
            BookingConfirmedEvent event = convertPayload(payload, BookingConfirmedEvent.class);
            log.info("[ShowCacheConsumer] BookingConfirmed – evicting seats cache for showId={}", event.showId());
            showCacheService.evictSeatAvailabilityCache(event.showId());
            showCacheMetrics.recordEviction("showSeats", "booking_confirmed");
        } catch (Exception e) {
            log.warn("[ShowCacheConsumer] Failed to process BookingConfirmedEvent for cache eviction", e);
        }
    }

    /**
     * Evicts the {@code showSeats} cache entry when a booking is cancelled
     * (seats return to AVAILABLE).
     */
    @KafkaListener(
            topics = ShowKafkaConfig.BOOKING_CANCELLED_TOPIC,
            groupId = ShowKafkaConfig.CONSUMER_GROUP_ID,
            containerFactory = "showKafkaListenerContainerFactory"
    )
    public void onBookingCancelled(@Payload Object payload) {
        try {
            BookingCancelledEvent event = convertPayload(payload, BookingCancelledEvent.class);
            log.info("[ShowCacheConsumer] BookingCancelled – evicting seats cache for showId={}", event.showId());
            showCacheService.evictSeatAvailabilityCache(event.showId());
            showCacheMetrics.recordEviction("showSeats", "booking_cancelled");
        } catch (Exception e) {
            log.warn("[ShowCacheConsumer] Failed to process BookingCancelledEvent for cache eviction", e);
        }
    }

    /**
     * Evicts the {@code showSeats} cache entry when a booking expires
     * (seats return to AVAILABLE).
     */
    @KafkaListener(
            topics = ShowKafkaConfig.BOOKING_EXPIRED_TOPIC,
            groupId = ShowKafkaConfig.CONSUMER_GROUP_ID,
            containerFactory = "showKafkaListenerContainerFactory"
    )
    public void onBookingExpired(@Payload Object payload) {
        try {
            BookingExpiredEvent event = convertPayload(payload, BookingExpiredEvent.class);
            log.info("[ShowCacheConsumer] BookingExpired – evicting seats cache for showId={}", event.showId());
            showCacheService.evictSeatAvailabilityCache(event.showId());
            showCacheMetrics.recordEviction("showSeats", "booking_expired");
        } catch (Exception e) {
            log.warn("[ShowCacheConsumer] Failed to process BookingExpiredEvent for cache eviction", e);
        }
    }

    // -------------------------------------------------------------------------
    // MOVIE EVENTS
    // -------------------------------------------------------------------------

    /**
     * Evicts all {@code shows} and {@code showSeats} cache entries for every show
     * belonging to the updated movie. Show details may include movie metadata (e.g.
     * duration), so stale entries must be purged.
     */
    @KafkaListener(
            topics = ShowKafkaConfig.MOVIE_UPDATED_TOPIC,
            groupId = ShowKafkaConfig.CONSUMER_GROUP_ID,
            containerFactory = "showKafkaListenerContainerFactory"
    )
    public void onMovieUpdated(@Payload Object payload) {
        try {
            MovieUpdatedEvent event = convertPayload(payload, MovieUpdatedEvent.class);
            log.info("[ShowCacheConsumer] MovieUpdated – evicting all show caches for movieId={}", event.movieId());
            showCacheService.evictCachesForMovie(event.movieId());
            showCacheMetrics.recordEviction("shows", "movie_updated");
            showCacheMetrics.recordEviction("showSeats", "movie_updated");
        } catch (Exception e) {
            log.warn("[ShowCacheConsumer] Failed to process MovieUpdatedEvent for cache eviction", e);
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <T> T convertPayload(Object payload, Class<T> targetType) {
        if (targetType.isInstance(payload)) {
            return targetType.cast(payload);
        }
        // When Jackson type headers are not present the deserializer returns a LinkedHashMap
        return objectMapper.convertValue(payload, targetType);
    }
}
