package com.krushna.moviebooking.show.repository;

import com.krushna.moviebooking.show.entity.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    List<ShowSeat> findByShowId(UUID showId);

    List<ShowSeat> findByShowIdAndStatus(UUID showId, String status);

    List<ShowSeat> findByIdIn(List<UUID> ids);

    List<ShowSeat> findByShowIdAndIdIn(UUID showId, List<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.id IN :ids")
    List<ShowSeat> findByShowIdAndIdInWithLock(@Param("showId") UUID showId, @Param("ids") List<UUID> ids);

    @Query("SELECT ss FROM ShowSeat ss WHERE ss.status = :status AND ss.lockExpiration < :now")
    List<ShowSeat> findExpiredLocks(@Param("status") String status, @Param("now") Instant now);
}
