package com.krushna.moviebooking.booking.repository;

import com.krushna.moviebooking.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByUserId(UUID userId, Pageable pageable);

    List<Booking> findByShowIdAndStatus(UUID showId, String status);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("status") String status, @Param("now") Instant now);
}
