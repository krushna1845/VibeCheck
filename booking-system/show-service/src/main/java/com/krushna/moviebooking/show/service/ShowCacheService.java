package com.krushna.moviebooking.show.service;

import java.util.UUID;

public interface ShowCacheService {

    void evictShowDetailsCache(UUID showId);

    void evictSeatAvailabilityCache(UUID showId);

    void evictAllCachesForShow(UUID showId);

    void evictCachesForMovie(UUID movieId);

    void evictAllShowCaches();
}
