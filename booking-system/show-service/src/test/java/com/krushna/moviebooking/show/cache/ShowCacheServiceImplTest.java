package com.krushna.moviebooking.show.cache;

import com.krushna.moviebooking.show.config.RedisCacheConfig;
import com.krushna.moviebooking.show.entity.Show;
import com.krushna.moviebooking.show.repository.ShowRepository;
import com.krushna.moviebooking.show.service.impl.ShowCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ShowCacheServiceImpl} – verifies correct interaction with
 * the underlying {@link CacheManager} for per-show and per-movie eviction scenarios.
 */
@ExtendWith(MockitoExtension.class)
class ShowCacheServiceImplTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private Cache showsCache;

    @Mock
    private Cache seatsCache;

    private ShowCacheServiceImpl cacheService;

    private UUID showId;
    private UUID movieId;

    @BeforeEach
    void setUp() {
        cacheService = new ShowCacheServiceImpl(cacheManager, showRepository);
        showId  = UUID.randomUUID();
        movieId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // evictAllShowCaches
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("evictAllShowCaches – clears both shows and showSeats caches")
    void evictAllShowCaches_clearsBothCaches() {
        when(cacheManager.getCache(RedisCacheConfig.SHOWS_CACHE)).thenReturn(showsCache);
        when(cacheManager.getCache(RedisCacheConfig.SHOW_SEATS_CACHE)).thenReturn(seatsCache);

        cacheService.evictAllShowCaches();

        verify(showsCache).clear();
        verify(seatsCache).clear();
    }

    @Test
    @DisplayName("evictAllShowCaches – handles null cache gracefully")
    void evictAllShowCaches_handlesNullCache() {
        when(cacheManager.getCache(RedisCacheConfig.SHOWS_CACHE)).thenReturn(null);
        when(cacheManager.getCache(RedisCacheConfig.SHOW_SEATS_CACHE)).thenReturn(null);

        // Should not throw
        cacheService.evictAllShowCaches();

        verifyNoInteractions(showsCache, seatsCache);
    }

    // -------------------------------------------------------------------------
    // evictCachesForMovie
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("evictCachesForMovie – evicts each show's cache entries individually")
    void evictCachesForMovie_evictsEachShowEntry() {
        UUID showId1 = UUID.randomUUID();
        UUID showId2 = UUID.randomUUID();

        Show show1 = Show.builder().id(showId1).movieId(movieId).build();
        Show show2 = Show.builder().id(showId2).movieId(movieId).build();

        when(showRepository.findByMovieIdAndDeletedAtIsNull(movieId)).thenReturn(List.of(show1, show2));
        when(cacheManager.getCache(RedisCacheConfig.SHOWS_CACHE)).thenReturn(showsCache);
        when(cacheManager.getCache(RedisCacheConfig.SHOW_SEATS_CACHE)).thenReturn(seatsCache);

        cacheService.evictCachesForMovie(movieId);

        verify(showsCache).evict(showId1);
        verify(showsCache).evict(showId2);
        verify(seatsCache).evict(showId1);
        verify(seatsCache).evict(showId2);
    }

    @Test
    @DisplayName("evictCachesForMovie – does nothing when no shows exist for movie")
    void evictCachesForMovie_noShowsForMovie() {
        when(showRepository.findByMovieIdAndDeletedAtIsNull(movieId)).thenReturn(List.of());
        when(cacheManager.getCache(RedisCacheConfig.SHOWS_CACHE)).thenReturn(showsCache);
        when(cacheManager.getCache(RedisCacheConfig.SHOW_SEATS_CACHE)).thenReturn(seatsCache);

        cacheService.evictCachesForMovie(movieId);

        verify(showsCache, never()).evict(any());
        verify(seatsCache, never()).evict(any());
    }

    @Test
    @DisplayName("evictCachesForMovie – falls back to clearing all caches on repository error")
    void evictCachesForMovie_fallsBackOnError() {
        when(showRepository.findByMovieIdAndDeletedAtIsNull(movieId))
                .thenThrow(new RuntimeException("DB error"));
        when(cacheManager.getCache(RedisCacheConfig.SHOWS_CACHE)).thenReturn(showsCache);
        when(cacheManager.getCache(RedisCacheConfig.SHOW_SEATS_CACHE)).thenReturn(seatsCache);

        // Should not throw – falls back to clearing all
        cacheService.evictCachesForMovie(movieId);

        verify(showsCache).clear();
        verify(seatsCache).clear();
    }
}
