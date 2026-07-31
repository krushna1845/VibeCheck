package com.krushna.moviebooking.booking.statemachine;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable value object that encodes a single entry in the booking state-transition table.
 *
 * <p>Each {@link BookingTransitionRule} declares:
 * <ul>
 *   <li><b>from</b> – the state the booking must currently be in</li>
 *   <li><b>allowedTargets</b> – the set of states the booking may legally move into</li>
 *   <li><b>description</b> – a human-readable rationale used in logs and diagnostics</li>
 * </ul>
 *
 * <p>Rules are registered in {@link BookingTransitionValidator} and evaluated by
 * {@link BookingStateMachine} on every {@code transition()} call.
 *
 * <p>This class is intentionally a plain Java record for immutability, structural
 * equality, and conciseness; it carries no Spring or JPA dependencies.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * BookingTransitionRule rule = new BookingTransitionRule(
 *     BookingStatus.CREATED,
 *     Set.of(BookingStatus.SEATS_LOCKED, BookingStatus.CANCELLED, BookingStatus.EXPIRED),
 *     "CREATED allows seat locking, user cancellation, or timeout expiry"
 * );
 * }</pre>
 */
public record BookingTransitionRule(
        BookingStatus from,
        Set<BookingStatus> allowedTargets,
        String description
) {

    // -----------------------------------------------------------------------
    // Compact canonical constructor – validation
    // -----------------------------------------------------------------------

    /**
     * Validates that mandatory fields are non-null and that the {@code from}
     * status is not terminal (terminal states must have no outgoing transitions).
     */
    public BookingTransitionRule {
        Objects.requireNonNull(from,           "BookingTransitionRule.from must not be null");
        Objects.requireNonNull(allowedTargets, "BookingTransitionRule.allowedTargets must not be null");
        Objects.requireNonNull(description,    "BookingTransitionRule.description must not be null");

        if (from.isTerminal() && !allowedTargets.isEmpty()) {
            throw new IllegalArgumentException(
                    "Terminal status " + from + " must not declare outgoing transitions, " +
                    "but found: " + allowedTargets);
        }

        // Defensive copy to guarantee immutability
        allowedTargets = Set.copyOf(allowedTargets);
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code target} is an allowed next state from {@link #from()}.
     *
     * @param target the status to test
     * @return {@code true} when the transition is permitted by this rule
     */
    public boolean allows(BookingStatus target) {
        return target != null && allowedTargets.contains(target);
    }

    /**
     * Returns {@code true} when there are no permitted outgoing transitions
     * (i.e. this rule is associated with a terminal state).
     */
    public boolean isTerminalRule() {
        return allowedTargets.isEmpty();
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    @Override
    public String toString() {
        return "BookingTransitionRule{from=" + from +
               ", allowedTargets=" + allowedTargets +
               ", description='" + description + "'}";
    }
}
