# Redis Caching for Show Service

## Overview

Milestone 6 introduces Redis-backed caching to the `show-service` using **Spring Cache** (`@Cacheable`, `@CacheEvict`, `@Caching`).  
Cache entries are automatically invalidated by consuming Kafka events from the booking and movie services, and by direct `@CacheEvict` annotations on write paths.  
Custom **Micrometer** metrics are exposed for cache eviction tracking through the Actuator endpoint.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                            show-service                              │
│                                                                      │
│  ┌──────────────┐    @Cacheable/@CacheEvict    ┌──────────────────┐ │
│  │ShowController│ ──────────────────────────► │  ShowServiceImpl  │ │
│  └──────────────┘                              │  (Spring Cache)   │ │
│                                                └────────┬─────────┘ │
│                                                         │           │
│                                                   Redis Cache       │
│                                                  ┌──────┴──────┐   │
│  ┌──────────────────────────┐                    │   shows      │   │
│  │  ShowCacheEventConsumer  │◄── Kafka events    │  showSeats   │   │
│  │  (cache invalidator)     │                    └─────────────┘   │
│  └──────────────────────────┘                                       │
│           │                                                         │
│  ┌────────▼─────────┐                                               │
│  │  ShowCacheService │ (programmatic eviction for movie-level ops)  │
│  └──────────────────┘                                               │
│                                                                      │
│  ┌──────────────────┐                                               │
│  │  ShowCacheMetrics│ ── Micrometer counters ► Actuator /metrics   │
│  └──────────────────┘                                               │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Cache Names & Keys

| Cache Name  | Key          | Cached Type                  | TTL (default) |
|-------------|--------------|------------------------------|---------------|
| `shows`     | `{showId}`   | `ShowResponse`               | 10 minutes    |
| `showSeats` | `{showId}`   | `List<ShowSeatResponse>`     | 10 minutes    |

TTL is configurable via:
```yaml
show:
  cache:
    ttl: 600s  # ISO-8601 duration
```

---

## Cached Operations

| Method                            | Cache Annotation           | Effect                                |
|-----------------------------------|----------------------------|---------------------------------------|
| `ShowServiceImpl.getShowById(id)` | `@Cacheable("shows")`      | Cache hit on subsequent calls         |
| `ShowServiceImpl.getShowSeats(showId)` | `@Cacheable("showSeats")` | Cache hit on subsequent calls        |
| `ShowServiceImpl.updateShow(id, ...)` | `@CacheEvict` both caches | Evicts shows + showSeats for `id`    |
| `ShowServiceImpl.cancelShow(id)`  | `@CacheEvict` both caches  | Evicts shows + showSeats for `id`    |

---

## Cache Invalidation Events

### Booking Events (from `booking-service`)

| Kafka Topic               | Event                    | Cache Evicted   | Reason              |
|---------------------------|--------------------------|-----------------|---------------------|
| `booking-created-events`  | `BookingCreatedEvent`    | `showSeats`     | Seats reserved      |
| `booking-confirmed-events`| `BookingConfirmedEvent`  | `showSeats`     | Seats confirmed     |
| `booking-cancelled-events`| `BookingCancelledEvent`  | `showSeats`     | Seats released      |
| `booking-expired-events`  | `BookingExpiredEvent`    | `showSeats`     | Seats released      |

### Movie Events (from `movie-service`)

| Kafka Topic             | Event               | Cache Evicted           | Reason                          |
|-------------------------|---------------------|-------------------------|---------------------------------|
| `movie-updated-events`  | `MovieUpdatedEvent` | `shows` + `showSeats`   | Show duration/title may change  |

---

## Configuration

### `application.yml`

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 4
        min-idle: 1

show:
  cache:
    ttl: 600s   # 10 min; use ISO-8601 duration string

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,caches,prometheus
  metrics:
    distribution:
      percentiles-histogram:
        cache.gets: true
```

---

## Redis Cache Configuration

`RedisCacheConfig.java` (`@EnableCaching`):
- Uses `GenericJackson2JsonRedisSerializer` with `JavaTimeModule` for `Instant`/`LocalDate` support.
- Activates Jackson **default typing** (`NON_FINAL, AS_PROPERTY`) so polymorphic types round-trip correctly.
- Enables **statistics** on `RedisCacheManager` (feeds Micrometer standard `cache.*` meters).
- Configures `disableCachingNullValues()` to prevent null entries from polluting Redis.

---

## Metrics

### Standard Spring Cache Meters (via `RedisCacheManager.enableStatistics()`)

| Metric Name         | Tags                      | Description                           |
|---------------------|---------------------------|---------------------------------------|
| `cache.gets`        | `cache`, `result` (hit/miss) | Cache lookups with hit/miss breakdown |
| `cache.puts`        | `cache`                   | Cache puts (new entries stored)       |
| `cache.evictions`   | `cache`                   | Cache evictions (standard)            |

### Custom Domain Eviction Meters (`ShowCacheMetrics`)

| Metric Name              | Tags                    | Description                                       |
|--------------------------|-------------------------|---------------------------------------------------|
| `show.cache.evictions`   | `cache`, `reason`       | Domain-event-driven evictions with reason tagging |
| `show.cache.eviction.total` | `cache`              | Gauge: total evictions per cache name             |

**Reasons**: `booking_created`, `booking_confirmed`, `booking_cancelled`, `booking_expired`, `movie_updated`.

### Actuator Endpoints

```
GET /actuator/metrics
GET /actuator/metrics/cache.gets
GET /actuator/metrics/show.cache.evictions
GET /actuator/caches
GET /actuator/health
```

---

## Key Classes

| Class                        | Package                                            | Role                                                     |
|------------------------------|----------------------------------------------------|----------------------------------------------------------|
| `RedisCacheConfig`           | `show.config`                                      | Spring Cache + Redis configuration, `@EnableCaching`     |
| `ShowKafkaConfig`            | `show.config`                                      | Kafka consumer/producer factory for show-service         |
| `ShowServiceImpl`            | `show.service.impl`                                | `@Cacheable` on reads, `@CacheEvict` on writes           |
| `ShowCacheService`           | `show.service`                                     | Programmatic cache eviction interface                    |
| `ShowCacheServiceImpl`       | `show.service.impl`                                | Implements per-show and per-movie eviction logic         |
| `ShowCacheEventConsumer`     | `show.kafka`                                       | Kafka listeners driving cache invalidation               |
| `ShowCacheMetrics`           | `show.metrics`                                     | Micrometer counters for domain-level eviction reasons    |

---

## Movie Service Changes

To trigger cache invalidation on movie updates, the `movie-service` now:

1. Publishes `MovieUpdatedEvent` (via `MovieEventPublisher`) to topic `movie-updated-events` on:
   - `updateMovie()`
   - `changeMovieStatus()`
   - `deleteMovie()` (soft delete)

2. A new `KafkaConfig` bean provisions the `movie-updated-events` topic.

---

## Test Coverage

| Test Class                   | Scenarios                                                                                      |
|------------------------------|-----------------------------------------------------------------------------------------------|
| `ShowCacheTest`              | All 4 booking events → seat eviction; movie event → show+seat eviction; metric counters      |
| `ShowCacheServiceImplTest`   | `evictAllShowCaches`, `evictCachesForMovie` (happy path, empty, fallback on DB error)        |
| `ShowServiceImplTest`        | Existing 9 tests (unchanged; still pass with cache annotations)                              |
| `MovieServiceImplTest`       | Existing 5 tests (mock `MovieEventPublisher` injected via `@Mock`)                           |

Run tests:
```bash
# show-service only
mvn test -pl show-service

# All affected modules
mvn test -pl common,movie-service,show-service
```

---

## Cache Invalidation Decision Matrix

| Event                | `shows` cache | `showSeats` cache | Notes                                   |
|----------------------|:---:|:---:|------------------------------------------|
| Show created         | ✗   | ✗   | Nothing to evict yet                    |
| Show updated         | ✓   | ✓   | Direct `@CacheEvict` in `updateShow`    |
| Show cancelled       | ✓   | ✓   | Direct `@CacheEvict` in `cancelShow`    |
| Booking created      | ✗   | ✓   | Seats reserved; show details unchanged  |
| Booking confirmed    | ✗   | ✓   | Seats confirmed; show details unchanged |
| Booking cancelled    | ✗   | ✓   | Seats released; show details unchanged  |
| Booking expired      | ✗   | ✓   | Seats released; show details unchanged  |
| Movie updated/deleted| ✓   | ✓   | Duration/title change affects show data |
| TTL expiry           | ✓   | ✓   | Auto-expiry after `show.cache.ttl`      |
