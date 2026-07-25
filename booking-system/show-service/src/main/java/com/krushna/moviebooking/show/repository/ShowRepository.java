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

    List<Show> findByMovieIdAndTheatreIdAndStartTimeBetween(UUID movieId, UUID theatreId, Instant start, Instant end);

    Page<Show> findByMovieIdAndStartTimeAfter(UUID movieId, Instant startTime, Pageable pageable);

    Page<Show> findByTheatreIdAndStartTimeBetween(UUID theatreId, Instant start, Instant end, Pageable pageable);

    @Query("SELECT s FROM Show s WHERE s.screenId = :screenId AND s.status = 'SCHEDULED' AND ((s.startTime <= :endTime AND s.endTime >= :startTime))")
    List<Show> findConflictingShows(@Param("screenId") UUID screenId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
