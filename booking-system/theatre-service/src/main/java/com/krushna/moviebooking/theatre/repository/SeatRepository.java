package com.krushna.moviebooking.theatre.repository;

import com.krushna.moviebooking.theatre.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByScreenId(UUID screenId);

    List<Seat> findByScreenIdAndIsActiveTrue(UUID screenId);

    List<Seat> findByScreenIdAndSeatCategory(UUID screenId, String seatCategory);

    Optional<Seat> findByScreenIdAndSeatRowIgnoreCaseAndSeatNumber(UUID screenId, String seatRow, Integer seatNumber);

    boolean existsByScreenIdAndSeatRowIgnoreCaseAndSeatNumber(UUID screenId, String seatRow, Integer seatNumber);

    long countByScreenIdAndIsActiveTrue(UUID screenId);
}
