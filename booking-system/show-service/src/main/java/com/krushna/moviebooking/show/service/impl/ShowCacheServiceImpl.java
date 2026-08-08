package com.krushna.moviebooking.show.service.impl;

import com.krushna.moviebooking.show.config.RedisCacheConfig;
import com.krushna.moviebooking.show.entity.Show;
import com.krushna.moviebooking.show.repository.ShowRepository;
import com.krushna.moviebooking.show.service.ShowCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowCacheServiceImpl implements ShowCacheService {

    private final CacheManager cacheManager;
    private final ShowRepository showRepository;

    @Override
    @CacheEvict(value = RedisCacheConfig.SHOWS_CACHE, key = "#showId")
    public void evictShowDetailsCache(UUID showId) {
        log.info("Evicted show details cache for showId: {}", showId);
    }

    @Override
    @CacheEvict(value = RedisCacheConfig.SHOW_SEATS_CACHE, key = "#showId")
    public void evictSeatAvailabilityCache(UUID showId) {
        log.info("Evicted seat availability cache for showId: {}", showId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisCacheConfig.SHOWS_CACHE, key = "#showId"),
            @CacheEvict(value = RedisCacheConfig.SHOW_SEATS_CACHE, key = "#showId")
    })
    public void evictAllCachesForShow(UUID showId) {
        log.info("Evicted all cache entries for showId: {}", showId);
    }

    @Override
    public void evictCachesForMovie(UUID movieId) {
        log.info("Evicting show caches for movieId: {}", movieId);
        try {
            List<Show> shows = showRepository.findByMovieIdAndDeletedAtIsNull(movieId);
            Cache showsCache = cacheManager.getCache(RedisCacheConfig.SHOWS_CACHE);
            Cache seatsCache = cacheManager.getCache(RedisCacheConfig.SHOW_SEATS_CACHE);

            for (Show show : shows) {
                if (showsCache != null) {
                    showsCache.evict(show.getId());
                }
                if (seatsCache != null) {
                    seatsCache.evict(show.getId());
                }
            }
            log.info("Evicted caches for {} shows associated with movieId: {}", shows.size(), movieId);
        } catch (Exception e) {
            log.warn("Error during movie cache eviction for movieId: {}, falling back to clearing cache", movieId, e);
            evictAllShowCaches();
        }
    }

    @Override
    public void evictAllShowCaches() {
        log.info("Clearing all show and seat availability caches");
        Cache showsCache = cacheManager.getCache(RedisCacheConfig.SHOWS_CACHE);
        if (showsCache != null) {
            showsCache.clear();
        }
        Cache seatsCache = cacheManager.getCache(RedisCacheConfig.SHOW_SEATS_CACHE);
        if (seatsCache != null) {
            seatsCache.clear();
        }
    }
}
