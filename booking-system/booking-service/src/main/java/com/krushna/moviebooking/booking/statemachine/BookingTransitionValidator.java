package com.krushna.moviebooking.booking.statemachine;

import java.util.Collection;
import java.util.Optional;

/**
 * Strategy interface for validating booking state transitions.
 *
 * <p>Implementations are responsible for:
 * <ol>
 *   <li>Holding the complete set of {@link BookingTransitionRule} objects that define
 *       every permitted transition in the booking state machine.</li>
 *   <li>Deciding whether a requested {@code from → to} pair is legal.</li>
 *   <li>Throwing {@link BookingTransitionException} when a transition is illegal.</li>
 * </ol>
 *
 * <p>The interface is intentionally kept thin so that alternative validation
 * strategies (rule-database, DSL, Spring-State-Machine adapter) can be plugged
 * in without changing the {@link BookingStateMachine}.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #validate} must throw {@link BookingTransitionException} for any illegal
 *       transition – it must never return normally in that case.</li>
 *   <li>{@link #isAllowed} must be a pure function with no side effects.</li>
 *   <li>{@link #getRuleFor} must be idempotent and thread-safe.</li>
 * </ul>
 */
public interface BookingTransitionValidator {

    // -----------------------------------------------------------------------
    // Core validation (throws on violation)
    // -----------------------------------------------------------------------

    /**
     * Validates that the transition from {@code from} to {@code to} is legal.
     *
     * @param from             current booking status
     * @param to               target booking status
     * @throws BookingTransitionException if the transition is not permitted
     */
    void validate(BookingStatus from, BookingStatus to);

    /**
     * Validates that the transition from {@code from} to {@code to} is legal,
     * including booking reference in the exception message for traceability.
     *
     * @param from             current booking status
     * @param to               target booking status
     * @param bookingReference booking reference code used in the error message
     * @throws BookingTransitionException if the transition is not permitted
     */
    void validate(BookingStatus from, BookingStatus to, String bookingReference);

    // -----------------------------------------------------------------------
    // Query (does not throw)
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the transition from {@code from} to {@code to}
     * is permitted by the registered rule set, {@code false} otherwise.
     *
     * <p>This method never throws; use it for conditional checks without
     * triggering exception handling overhead.
     *
     * @param from current booking status
     * @param to   target booking status
     * @return {@code true} when the transition is permitted
     */
    boolean isAllowed(BookingStatus from, BookingStatus to);

    // -----------------------------------------------------------------------
    // Rule introspection
    // -----------------------------------------------------------------------

    /**
     * Returns the {@link BookingTransitionRule} registered for the given {@code status},
     * if any.
     *
     * @param status the state whose rule is requested
     * @return {@link Optional} containing the rule, or empty if none is registered
     */
    Optional<BookingTransitionRule> getRuleFor(BookingStatus status);

    /**
     * Returns an unmodifiable view of all registered {@link BookingTransitionRule} objects.
     * Useful for diagnostics, admin endpoints, and documentation generation.
     *
     * @return immutable collection of all rules
     */
    Collection<BookingTransitionRule> getAllRules();
}
