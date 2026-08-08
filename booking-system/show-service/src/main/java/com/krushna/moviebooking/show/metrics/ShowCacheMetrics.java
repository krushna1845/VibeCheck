package com.krushna.moviebooking.show.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Micrometer-based cache metrics for the Show Service Redis caches.
 *
 * <h2>Metrics exposed</h2>
 * <ul>
 *   <li>{@code show.cache.evictions} – counter tagged with {@code cache} (shows/showSeats)
 *       and {@code reason} (booking_created, booking_confirmed, booking_cancelled,
 *       booking_expired, movie_updated, show_updated, show_cancelled).</li>
 *   <li>{@code show.cache.size} – gauge per named cache exposed via Actuator.</li>
 * </ul>
 *
 * <p>Spring's {@code RedisCacheManager#enableStatistics()} exposes standard
 * {@code cache.gets}, {@code cache.puts}, and {@code cache.evictions} counters
 * automatically through the Actuator {@code /actuator/metrics} endpoint.
 * This class adds domain-specific eviction reason tagging on top.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShowCacheMetrics {

    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;

    /**
     * Tracks eviction reason counts per (cache, reason) pair.
     * Key format: "cacheName::reason"
     */
    private final ConcurrentHashMap<String, AtomicLong> evictionCounts = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerGauges() {
        // Register a gauge for each named cache size when using RedisCacheManager
        if (cacheManager instanceof RedisCacheManager rcm) {
            rcm.getCacheNames().forEach(cacheName ->
                    Gauge.builder("show.cache.eviction.total", evictionCounts,
                                    map -> map.entrySet().stream()
                                            .filter(e -> e.getKey().startsWith(cacheName + "::"))
                                            .mapToLong(e -> e.getValue().get())
                                            .sum())
                            .description("Total evictions for cache: " + cacheName)
                            .tag("cache", cacheName)
                            .register(meterRegistry)
            );
        }
        log.info("ShowCacheMetrics gauges registered");
    }

    /**
     * Records an eviction event, incrementing both a Micrometer Counter and the
     * internal evictionCounts map used by Gauges.
     *
     * @param cacheName the name of the cache (e.g. {@code shows}, {@code showSeats})
     * @param reason    a human-readable reason tag (e.g. {@code booking_created})
     */
    public void recordEviction(String cacheName, String reason) {
        // Micrometer Counter – auto-creates on first call
        Counter.builder("show.cache.evictions")
                .description("Number of cache evictions triggered by domain events")
                .tag("cache", cacheName)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();

        // Internal count for Gauge
        String key = cacheName + "::" + reason;
        evictionCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();

        log.debug("[ShowCacheMetrics] eviction recorded: cache={}, reason={}", cacheName, reason);
    }
}
