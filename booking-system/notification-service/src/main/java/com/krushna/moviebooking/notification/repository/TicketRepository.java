package com.krushna.moviebooking.notification.repository;

import com.krushna.moviebooking.notification.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByBookingId(UUID bookingId);
    Optional<Ticket> findByBookingReference(String bookingReference);
    Optional<Ticket> findByTicketNumber(String ticketNumber);
}
