package com.krushna.moviebooking.booking.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link OutboxEvent}.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
