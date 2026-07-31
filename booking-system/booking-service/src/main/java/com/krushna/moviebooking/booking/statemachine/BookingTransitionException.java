package com.krushna.moviebooking.booking.statemachine;

/**
 * Thrown when an illegal or unsupported state transition is attempted on a Booking.
 *
 * <p>This exception is the canonical signal from the {@link BookingStateMachine} and
 * {@link BookingTransitionValidator} whenever a transition is rejected. Callers should
 * treat this as an unrecoverable business-rule violation; the booking status must not
 * be modified and the surrounding transaction should be rolled back.
 *
 * <p>The exception preserves the {@code fromStatus} and {@code toStatus} that caused
 * the violation so that upstream handlers (e.g. {@code GlobalExceptionHandler}) can
 * surface a precise error message to the API consumer.
 *
 * <p>Example usages:
 * <ul>
 *   <li>Attempting {@code COMPLETED → PAYMENT_PENDING} (workflow already finished)</li>
 *   <li>Attempting {@code FAILED → CONFIRMED} (failed bookings require new booking)</li>
 *   <li>Attempting any transition out of a terminal state</li>
 * </ul>
 */
public class BookingTransitionException extends RuntimeException {

    /** Status the booking was in when the illegal transition was attempted. */
    private final BookingStatus fromStatus;

    /** Status the caller attempted to transition into. */
    private final BookingStatus toStatus;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link BookingTransitionException} with a plain message only.
     * Use when the transition cannot be expressed as a simple from→to pair
     * (e.g. invalid / unknown status value).
     *
     * @param message descriptive error message
     */
    public BookingTransitionException(String message) {
        super(message);
        this.fromStatus = null;
        this.toStatus   = null;
    }

    /**
     * Creates a {@link BookingTransitionException} for a specific illegal transition.
     *
     * @param fromStatus current booking status
     * @param toStatus   target booking status that was rejected
     */
    public BookingTransitionException(BookingStatus fromStatus, BookingStatus toStatus) {
        super(buildMessage(fromStatus, toStatus, null, null));
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
    }

    /**
     * Creates a {@link BookingTransitionException} for a specific illegal transition,
     * including the booking reference for traceability.
     *
     * @param fromStatus       current booking status
     * @param toStatus         target booking status that was rejected
     * @param bookingReference booking reference code (may be {@code null})
     */
    public BookingTransitionException(BookingStatus fromStatus, BookingStatus toStatus,
                                      String bookingReference) {
        super(buildMessage(fromStatus, toStatus, bookingReference, null));
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
    }

    /**
     * Creates a {@link BookingTransitionException} for a specific illegal transition,
     * including both a booking reference and an extra detail hint.
     *
     * @param fromStatus       current booking status
     * @param toStatus         target booking status that was rejected
     * @param bookingReference booking reference code (may be {@code null})
     * @param detail           additional contextual hint (may be {@code null})
     */
    public BookingTransitionException(BookingStatus fromStatus, BookingStatus toStatus,
                                      String bookingReference, String detail) {
        super(buildMessage(fromStatus, toStatus, bookingReference, detail));
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the status the booking held at the time of the illegal transition attempt.
     * May be {@code null} when the exception was constructed without enum arguments.
     */
    public BookingStatus getFromStatus() {
        return fromStatus;
    }

    /**
     * Returns the target status that was rejected.
     * May be {@code null} when the exception was constructed without enum arguments.
     */
    public BookingStatus getToStatus() {
        return toStatus;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String buildMessage(BookingStatus from, BookingStatus to,
                                       String ref, String detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("Illegal booking state transition: ")
          .append(from != null ? from.name() : "UNKNOWN")
          .append(" → ")
          .append(to   != null ? to.name()   : "UNKNOWN");

        if (ref != null && !ref.isBlank()) {
            sb.append(" [booking=").append(ref).append("]");
        }
        if (detail != null && !detail.isBlank()) {
            sb.append(" – ").append(detail);
        }
        return sb.toString();
    }
}
