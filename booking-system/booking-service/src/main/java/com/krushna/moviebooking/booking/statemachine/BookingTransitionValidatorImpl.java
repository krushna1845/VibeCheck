package com.krushna.moviebooking.booking.statemachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link BookingTransitionValidator}.
 *
 * <p>Registers the complete booking state-transition matrix as defined in
 * {@code docs/booking-engine/state-machine.md} and enforces every rule at runtime.
 *
 * <h2>Transition Matrix</h2>
 * <pre>
 * ┌──────────────────┬────────────────────────────────────────────┐
 * │  Current State   │  Allowed Next States                       │
 * ├──────────────────┼────────────────────────────────────────────┤
 * │  CREATED         │  SEATS_LOCKED, CANCELLED, EXPIRED          │
 * │  SEATS_LOCKED    │  PAYMENT_PENDING, FAILED, EXPIRED          │
 * │  PAYMENT_PENDING │  CONFIRMED, FAILED, EXPIRED                │
 * │  CONFIRMED       │  COMPLETED                                 │
 * │  COMPLETED       │  (none – terminal)                         │
 * │  FAILED          │  (none – terminal)                         │
 * │  CANCELLED       │  (none – terminal)                         │
 * │  EXPIRED         │  (none – terminal)                         │
 * └──────────────────┴────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Thread-safety: the rule map is populated once during construction from an
 * immutable set of rules and never mutated afterwards, making this component
 * safe for concurrent use without additional synchronisation.
 */
@Slf4j
@Component
public class BookingTransitionValidatorImpl implements BookingTransitionValidator {

    /** Immutable map of state → rule, populated once at construction time. */
    private final Map<BookingStatus, BookingTransitionRule> rules;

    // -----------------------------------------------------------------------
    // Construction – rule registration
    // -----------------------------------------------------------------------

    public BookingTransitionValidatorImpl() {
        Map<BookingStatus, BookingTransitionRule> mutable = new EnumMap<>(BookingStatus.class);

        // ── Non-terminal states with outgoing transitions ──────────────────

        mutable.put(BookingStatus.CREATED, new BookingTransitionRule(
                BookingStatus.CREATED,
                Set.of(BookingStatus.SEATS_LOCKED,
                       BookingStatus.CANCELLED,
                       BookingStatus.EXPIRED),
                "CREATED: seat locking may proceed; customer may cancel; scheduler may expire"
        ));

        mutable.put(BookingStatus.SEATS_LOCKED, new BookingTransitionRule(
                BookingStatus.SEATS_LOCKED,
                Set.of(BookingStatus.PAYMENT_PENDING,
                       BookingStatus.FAILED,
                       BookingStatus.EXPIRED),
                "SEATS_LOCKED: payment may be initiated; lock failure marks FAILED; scheduler may expire"
        ));

        mutable.put(BookingStatus.PAYMENT_PENDING, new BookingTransitionRule(
                BookingStatus.PAYMENT_PENDING,
                Set.of(BookingStatus.CONFIRMED,
                       BookingStatus.FAILED,
                       BookingStatus.EXPIRED),
                "PAYMENT_PENDING: payment success → CONFIRMED; payment failure → FAILED; timeout → EXPIRED"
        ));

        mutable.put(BookingStatus.CONFIRMED, new BookingTransitionRule(
                BookingStatus.CONFIRMED,
                Set.of(BookingStatus.COMPLETED),
                "CONFIRMED: post-confirmation workflow completes the booking"
        ));

        // ── Terminal states – empty allowed-target sets ────────────────────

        mutable.put(BookingStatus.COMPLETED, new BookingTransitionRule(
                BookingStatus.COMPLETED,
                Set.of(),
                "COMPLETED: terminal state – no further transitions permitted"
        ));

        mutable.put(BookingStatus.FAILED, new BookingTransitionRule(
                BookingStatus.FAILED,
                Set.of(),
                "FAILED: terminal state – booking requires a fresh attempt"
        ));

        mutable.put(BookingStatus.CANCELLED, new BookingTransitionRule(
                BookingStatus.CANCELLED,
                Set.of(),
                "CANCELLED: terminal state – cancelled bookings cannot be recovered"
        ));

        mutable.put(BookingStatus.EXPIRED, new BookingTransitionRule(
                BookingStatus.EXPIRED,
                Set.of(),
                "EXPIRED: terminal state – reservation window has closed"
        ));

        this.rules = Collections.unmodifiableMap(mutable);

        log.info("BookingTransitionValidator initialised with {} rules", rules.size());
    }

    // -----------------------------------------------------------------------
    // BookingTransitionValidator – core validation
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Throws {@link BookingTransitionException} when:
     * <ul>
     *   <li>Either argument is {@code null}</li>
     *   <li>{@code from} is a terminal state</li>
     *   <li>{@code to} is not listed in the rule for {@code from}</li>
     * </ul>
     */
    @Override
    public void validate(BookingStatus from, BookingStatus to) {
        validate(from, to, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Includes the {@code bookingReference} in the exception message when non-null.
     */
    @Override
    public void validate(BookingStatus from, BookingStatus to, String bookingReference) {
        if (from == null) {
            throw new BookingTransitionException(
                    "Cannot validate transition: current status (from) is null");
        }
        if (to == null) {
            throw new BookingTransitionException(
                    "Cannot validate transition: target status (to) is null");
        }

        // Rule 10: terminal states cannot transition to any other state.
        if (from.isTerminal()) {
            log.warn("Rejected transition from terminal state {} → {} [booking={}]",
                     from, to, bookingReference);
            throw new BookingTransitionException(from, to, bookingReference,
                    from + " is a terminal state and accepts no further transitions");
        }

        BookingTransitionRule rule = rules.get(from);

        if (rule == null) {
            // Defensive – every non-terminal state must have a rule.
            log.error("No transition rule registered for state {} – state machine is misconfigured", from);
            throw new BookingTransitionException(from, to, bookingReference,
                    "No rule registered for state " + from + "; state machine misconfiguration");
        }

        if (!rule.allows(to)) {
            log.warn("Rejected transition {} → {} [booking={}]. Allowed targets: {}",
                     from, to, bookingReference, rule.allowedTargets());
            throw new BookingTransitionException(from, to, bookingReference,
                    "Allowed transitions from " + from + ": " + rule.allowedTargets());
        }

        log.debug("Transition {} → {} validated successfully [booking={}]",
                  from, to, bookingReference);
    }

    // -----------------------------------------------------------------------
    // BookingTransitionValidator – query
    // -----------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean isAllowed(BookingStatus from, BookingStatus to) {
        if (from == null || to == null || from.isTerminal()) {
            return false;
        }
        BookingTransitionRule rule = rules.get(from);
        return rule != null && rule.allows(to);
    }

    // -----------------------------------------------------------------------
    // BookingTransitionValidator – rule introspection
    // -----------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Optional<BookingTransitionRule> getRuleFor(BookingStatus status) {
        return Optional.ofNullable(rules.get(status));
    }

    /** {@inheritDoc} */
    @Override
    public Collection<BookingTransitionRule> getAllRules() {
        return rules.values(); // already unmodifiable (Collections.unmodifiableMap values view)
    }
}
