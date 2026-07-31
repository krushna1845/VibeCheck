package com.krushna.moviebooking.booking.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ProcessedEvent} entities.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
