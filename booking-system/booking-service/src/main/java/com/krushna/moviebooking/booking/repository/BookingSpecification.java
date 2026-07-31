package com.krushna.moviebooking.booking.repository;

import com.krushna.moviebooking.booking.dto.BookingSearchCriteria;
import com.krushna.moviebooking.booking.entity.Booking;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA {@link Specification} factory for dynamic admin booking searches.
 *
 * <p>Builds an AND-chain of predicates from a {@link BookingSearchCriteria}.
 * Fields left {@code null} in the criteria contribute no predicate.
 */
public final class BookingSpecification {

    private BookingSpecification() {}

    /**
     * Builds a {@link Specification} from the given search criteria.
     *
     * @param criteria Admin search filter (all fields optional)
     * @return Composed specification with AND semantics
     */
    public static Specification<Booking> fromCriteria(BookingSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted bookings
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (criteria == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (criteria.status() != null && !criteria.status().isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("status")),
                        criteria.status().trim().toUpperCase()
                ));
            }

            if (criteria.userId() != null) {
                predicates.add(cb.equal(root.get("userId"), criteria.userId()));
            }

            if (criteria.showId() != null) {
                predicates.add(cb.equal(root.get("showId"), criteria.showId()));
            }

            if (criteria.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.dateFrom()));
            }

            if (criteria.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.dateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
