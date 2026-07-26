package com.krushna.moviebooking.show.repository;

import com.krushna.moviebooking.show.entity.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowRepository extends JpaRepository<Show, UUID> {

    List<Show> findByMovieIdAndDeletedAtIsNull(UUID movieId);

    Page<Show> findByMovieIdAndDeletedAtIsNull(UUID movieId, Pageable pageable);

    List<Show> findByScreenIdAndDeletedAtIsNull(UUID screenId);

    Page<Show> findByScreenIdAndDeletedAtIsNull(UUID screenId, Pageable pageable);

    List<Show> findByStartTimeBetweenAndDeletedAtIsNull(Instant start, Instant end);

    List<Show> findByTheatreIdAndStartTimeBetweenAndDeletedAtIsNull(UUID theatreId, Instant start, Instant end);

    List<Show> findByMovieIdAndTheatreIdAndStartTimeBetween(UUID movieId, UUID theatreId, Instant start, Instant end);

    Page<Show> findByMovieIdAndStartTimeAfter(UUID movieId, Instant startTime, Pageable pageable);

    Page<Show> findByTheatreIdAndStartTimeBetween(UUID theatreId, Instant start, Instant end, Pageable pageable);

    @Query("SELECT s FROM Show s WHERE s.screenId = :screenId AND s.status = 'SCHEDULED' AND s.deletedAt IS NULL AND s.startTime < :endTime AND s.endTime > :startTime AND (:excludeShowId IS NULL OR s.id != :excludeShowId)")
    List<Show> findConflictingShows(
            @Param("screenId") UUID screenId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("excludeShowId") UUID excludeShowId
    );
}
