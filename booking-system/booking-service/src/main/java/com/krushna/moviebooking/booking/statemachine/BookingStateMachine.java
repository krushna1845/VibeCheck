package com.krushna.moviebooking.booking.statemachine;

import com.krushna.moviebooking.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Core state machine for the booking lifecycle.
 *
 * <p>{@link BookingStateMachine} is the <em>single entry point</em> for all booking
 * status mutations in the system.  No component should update a booking's status
 * directly; every change must pass through {@link #transition(Booking, BookingStatus)}.
 *
 * <h2>Responsibilities</h2>
 * <ol>
 *   <li>Delegates transition legality to {@link BookingTransitionValidator}.</li>
 *   <li>Applies the approved status change atomically on the in-memory entity.</li>
 *   <li>Emits structured audit log entries for every transition attempt (success or failure).</li>
 *   <li>Supports string-based callers via {@link #transition(Booking, String)}.</li>
 *   <li>Provides idempotency helpers: {@link #isTerminal(Booking)}, {@link #canTransition}.</li>
 * </ol>
 *
 * <h2>Concurrency</h2>
 * <p>This component is stateless beyond its immutable {@link BookingTransitionValidator}
 * dependency, making it inherently thread-safe.  Concurrent callers contending on the
 * <em>same</em> booking entity must serialise access at the database layer using
 * optimistic or pessimistic locking on the {@link Booking} aggregate.
 *
 * <h2>Persistence contract</h2>
 * <p>{@link #transition} mutates the in-memory entity but does <strong>not</strong>
 * flush it to the database.  The calling service (e.g. {@code BookingServiceImpl})
 * is responsible for saving the entity within the enclosing transaction.
 *
 * <h2>Illegal-transition examples</h2>
 * <pre>
 *  CONFIRMED  → CREATED        – booking cannot restart
 *  COMPLETED  → PAYMENT_PENDING – workflow already finished
 *  FAILED     → CONFIRMED      – failed bookings require new booking
 *  EXPIRED    → PAYMENT_PENDING – reservation no longer valid
 *  CANCELLED  → CONFIRMED      – cancelled bookings cannot recover
 *  COMPLETED  → CANCELLED      – completed bookings cannot be cancelled
 *  CONFIRMED  → FAILED         – successful booking cannot fail afterwards
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingStateMachine {

    private final BookingTransitionValidator transitionValidator;

    // -----------------------------------------------------------------------
    // Primary transition methods
    // -----------------------------------------------------------------------

    /**
     * Transitions the supplied {@link Booking} entity to {@code targetStatus}.
     *
     * <p>Steps performed:
     * <ol>
     *   <li>Validates that {@code booking} and {@code targetStatus} are non-null.</li>
     *   <li>Resolves the current {@link BookingStatus} from the entity's string field.</li>
     *   <li>Delegates to {@link BookingTransitionValidator#validate} – throws
     *       {@link BookingTransitionException} for illegal transitions.</li>
     *   <li>Applies the new status on the entity.</li>
     *   <li>Logs the successful transition at {@code INFO} level.</li>
     * </ol>
     *
     * @param booking      booking entity to mutate (must not be {@code null})
     * @param targetStatus target state (must not be {@code null})
     * @throws BookingTransitionException if the transition is illegal
     * @throws IllegalArgumentException   if {@code booking} is {@code null}
     */
    public void transition(Booking booking, BookingStatus targetStatus) {
        Objects.requireNonNull(booking,      "Booking entity must not be null");
        Objects.requireNonNull(targetStatus, "Target BookingStatus must not be null");

        BookingStatus currentStatus = resolveCurrentStatus(booking);
        String ref = booking.getBookingReference();

        // Idempotency guard – already in target state, nothing to do.
        if (currentStatus == targetStatus) {
            log.debug("Idempotent transition: booking [{}] is already in status {}. No-op.",
                      ref, currentStatus);
            return;
        }

        log.info("Attempting booking state transition: {} → {} [booking={}]",
                 currentStatus, targetStatus, ref);

        // Delegate to validator – throws BookingTransitionException on illegal transition.
        transitionValidator.validate(currentStatus, targetStatus, ref);

        // Apply the transition on the entity.
        booking.setStatus(targetStatus.name());

        log.info("Booking state transition applied: {} → {} [booking={}]",
                 currentStatus, targetStatus, ref);
    }

    /**
     * String-based overload for callers that work with raw status strings
     * (e.g. services reading status from a Kafka payload or REST body).
     *
     * <p>Parses {@code targetStatusStr} via {@link BookingStatus#from(String)} before
     * delegating to {@link #transition(Booking, BookingStatus)}.
     *
     * @param booking         booking entity to mutate (must not be {@code null})
     * @param targetStatusStr target state as a case-insensitive string
     * @throws BookingTransitionException if the string is unrecognised or the
     *                                    transition is illegal
     */
    public void transition(Booking booking, String targetStatusStr) {
        Objects.requireNonNull(booking,         "Booking entity must not be null");
        Objects.requireNonNull(targetStatusStr, "Target status string must not be null");

        BookingStatus targetStatus = BookingStatus.from(targetStatusStr); // throws if unknown
        transition(booking, targetStatus);
    }

    // -----------------------------------------------------------------------
    // Query helpers (pure, no mutation)
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the booking is currently in a terminal state and
     * can accept no further transitions.
     *
     * @param booking booking entity to inspect
     * @return {@code true} when the booking is terminal
     */
    public boolean isTerminal(Booking booking) {
        Objects.requireNonNull(booking, "Booking entity must not be null");
        return resolveCurrentStatus(booking).isTerminal();
    }

    /**
     * Returns {@code true} if transitioning the given booking to {@code targetStatus}
     * would be legal according to the registered rule set.
     *
     * <p>This is a pure predicate that never throws and has no side effects.
     *
     * @param booking      booking entity to inspect
     * @param targetStatus candidate target state
     * @return {@code true} when the transition would be permitted
     */
    public boolean canTransition(Booking booking, BookingStatus targetStatus) {
        if (booking == null || targetStatus == null) {
            return false;
        }
        BookingStatus current = resolveCurrentStatus(booking);
        if (current == targetStatus) {
            return true; // idempotent
        }
        return transitionValidator.isAllowed(current, targetStatus);
    }

    /**
     * String-based overload of {@link #canTransition(Booking, BookingStatus)}.
     *
     * @param booking         booking entity to inspect
     * @param targetStatusStr candidate target state as a case-insensitive string
     * @return {@code true} when the transition would be permitted
     */
    public boolean canTransition(Booking booking, String targetStatusStr) {
        if (booking == null || targetStatusStr == null || targetStatusStr.isBlank()) {
            return false;
        }
        return BookingStatus.fromSafe(targetStatusStr)
                .map(target -> canTransition(booking, target))
                .orElse(false);
    }

    /**
     * Returns the current {@link BookingStatus} of the booking as a typed enum value.
     *
     * @param booking booking entity to inspect
     * @return current {@link BookingStatus}
     * @throws BookingTransitionException if the booking's status string is unrecognised
     */
    public BookingStatus currentStatus(Booking booking) {
        Objects.requireNonNull(booking, "Booking entity must not be null");
        return resolveCurrentStatus(booking);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the string status stored on the entity to a {@link BookingStatus} enum value.
     * Throws {@link BookingTransitionException} with a meaningful message if parsing fails.
     */
    private BookingStatus resolveCurrentStatus(Booking booking) {
        String rawStatus = booking.getStatus();
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new BookingTransitionException(
                    "Booking [" + booking.getBookingReference() + "] has a null or blank status field; " +
                    "the entity may be corrupted");
        }
        try {
            return BookingStatus.from(rawStatus);
        } catch (BookingTransitionException e) {
            log.error("Booking [{}] has an unrecognised status value: '{}'",
                      booking.getBookingReference(), rawStatus);
            throw new BookingTransitionException(
                    "Booking [" + booking.getBookingReference() + "] holds unrecognised status: '" +
                    rawStatus + "'. Possible data migration issue.");
        }
    }
}
