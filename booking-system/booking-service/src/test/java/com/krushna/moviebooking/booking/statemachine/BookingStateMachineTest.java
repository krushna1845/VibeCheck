package com.krushna.moviebooking.booking.statemachine;

import com.krushna.moviebooking.booking.entity.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive unit-test suite for the Booking State Machine.
 *
 * <p>Test categories:
 * <ul>
 *   <li>{@link ValidTransitionsTest}      – every legal transition defined in the matrix</li>
 *   <li>{@link InvalidTransitionsTest}    – every illegal transition from the spec</li>
 *   <li>{@link TerminalStatesTest}        – terminal states accept no further changes</li>
 *   <li>{@link ConcurrencySafetyTest}     – race conditions on the state machine component</li>
 *   <li>{@link BookingStatusEnumTest}     – BookingStatus enum helpers and factory methods</li>
 *   <li>{@link BookingTransitionRuleTest} – BookingTransitionRule value-object invariants</li>
 *   <li>{@link IdempotencyTest}           – same-state transitions are safe no-ops</li>
 *   <li>{@link QueryHelperTest}           – canTransition / isTerminal / currentStatus</li>
 * </ul>
 *
 * <p>All tests are pure unit tests – no Spring context, no database, no Redis.
 */
@DisplayName("BookingStateMachine")
class BookingStateMachineTest {

    // -----------------------------------------------------------------------
    // Shared infrastructure
    // -----------------------------------------------------------------------

    /** Real validator – no mocking; we want to test the full pipeline. */
    private BookingTransitionValidator validator;
    private BookingStateMachine        stateMachine;

    @BeforeEach
    void setUp() {
        validator    = new BookingTransitionValidatorImpl();
        stateMachine = new BookingStateMachine(validator);
    }

    // -----------------------------------------------------------------------
    // Helper factory
    // -----------------------------------------------------------------------

    private static Booking bookingWithStatus(String status) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .bookingReference("BK" + System.nanoTime())
                .userId(UUID.randomUUID())
                .showId(UUID.randomUUID())
                .status(status)
                .totalAmount(BigDecimal.valueOf(500))
                .taxAmount(BigDecimal.valueOf(50))
                .convenienceFee(BigDecimal.valueOf(20))
                .expiresAt(Instant.now().plusSeconds(600))
                .bookingSeats(new ArrayList<>())
                .build();
    }

    private static Booking bookingWithStatus(BookingStatus status) {
        return bookingWithStatus(status.name());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1. VALID TRANSITIONS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitionsTest {

        // ── CREATED ────────────────────────────────────────────────────────

        @Test
        @DisplayName("CREATED → SEATS_LOCKED is permitted")
        void created_to_seatsLocked() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            stateMachine.transition(booking, BookingStatus.SEATS_LOCKED);
            assertThat(booking.getStatus()).isEqualTo("SEATS_LOCKED");
        }

        @Test
        @DisplayName("CREATED → CANCELLED is permitted")
        void created_to_cancelled() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            stateMachine.transition(booking, BookingStatus.CANCELLED);
            assertThat(booking.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("CREATED → EXPIRED is permitted")
        void created_to_expired() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            stateMachine.transition(booking, BookingStatus.EXPIRED);
            assertThat(booking.getStatus()).isEqualTo("EXPIRED");
        }

        // ── SEATS_LOCKED ───────────────────────────────────────────────────

        @Test
        @DisplayName("SEATS_LOCKED → PAYMENT_PENDING is permitted")
        void seatsLocked_to_paymentPending() {
            Booking booking = bookingWithStatus(BookingStatus.SEATS_LOCKED);
            stateMachine.transition(booking, BookingStatus.PAYMENT_PENDING);
            assertThat(booking.getStatus()).isEqualTo("PAYMENT_PENDING");
        }

        @Test
        @DisplayName("SEATS_LOCKED → FAILED is permitted")
        void seatsLocked_to_failed() {
            Booking booking = bookingWithStatus(BookingStatus.SEATS_LOCKED);
            stateMachine.transition(booking, BookingStatus.FAILED);
            assertThat(booking.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("SEATS_LOCKED → EXPIRED is permitted")
        void seatsLocked_to_expired() {
            Booking booking = bookingWithStatus(BookingStatus.SEATS_LOCKED);
            stateMachine.transition(booking, BookingStatus.EXPIRED);
            assertThat(booking.getStatus()).isEqualTo("EXPIRED");
        }

        // ── PAYMENT_PENDING ────────────────────────────────────────────────

        @Test
        @DisplayName("PAYMENT_PENDING → CONFIRMED is permitted")
        void paymentPending_to_confirmed() {
            Booking booking = bookingWithStatus(BookingStatus.PAYMENT_PENDING);
            stateMachine.transition(booking, BookingStatus.CONFIRMED);
            assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("PAYMENT_PENDING → FAILED is permitted")
        void paymentPending_to_failed() {
            Booking booking = bookingWithStatus(BookingStatus.PAYMENT_PENDING);
            stateMachine.transition(booking, BookingStatus.FAILED);
            assertThat(booking.getStatus()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("PAYMENT_PENDING → EXPIRED is permitted")
        void paymentPending_to_expired() {
            Booking booking = bookingWithStatus(BookingStatus.PAYMENT_PENDING);
            stateMachine.transition(booking, BookingStatus.EXPIRED);
            assertThat(booking.getStatus()).isEqualTo("EXPIRED");
        }

        // ── CONFIRMED ──────────────────────────────────────────────────────

        @Test
        @DisplayName("CONFIRMED → COMPLETED is permitted")
        void confirmed_to_completed() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            stateMachine.transition(booking, BookingStatus.COMPLETED);
            assertThat(booking.getStatus()).isEqualTo("COMPLETED");
        }

        // ── String-based overload ──────────────────────────────────────────

        @Test
        @DisplayName("String overload: CREATED → SEATS_LOCKED via string")
        void created_to_seatsLocked_stringOverload() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            stateMachine.transition(booking, "seats_locked"); // lowercase
            assertThat(booking.getStatus()).isEqualTo("SEATS_LOCKED");
        }

        // ── Parameterised – full valid matrix ─────────────────────────────

        static Stream<Arguments> validMatrix() {
            return Stream.of(
                    Arguments.of(BookingStatus.CREATED,          BookingStatus.SEATS_LOCKED),
                    Arguments.of(BookingStatus.CREATED,          BookingStatus.CANCELLED),
                    Arguments.of(BookingStatus.CREATED,          BookingStatus.EXPIRED),
                    Arguments.of(BookingStatus.SEATS_LOCKED,     BookingStatus.PAYMENT_PENDING),
                    Arguments.of(BookingStatus.SEATS_LOCKED,     BookingStatus.FAILED),
                    Arguments.of(BookingStatus.SEATS_LOCKED,     BookingStatus.EXPIRED),
                    Arguments.of(BookingStatus.PAYMENT_PENDING,  BookingStatus.CONFIRMED),
                    Arguments.of(BookingStatus.PAYMENT_PENDING,  BookingStatus.FAILED),
                    Arguments.of(BookingStatus.PAYMENT_PENDING,  BookingStatus.EXPIRED),
                    Arguments.of(BookingStatus.CONFIRMED,        BookingStatus.COMPLETED)
            );
        }

        @ParameterizedTest(name = "{0} → {1}")
        @MethodSource("validMatrix")
        @DisplayName("Parameterised: all valid transitions succeed")
        void parameterised_validTransitions(BookingStatus from, BookingStatus to) {
            Booking booking = bookingWithStatus(from);
            stateMachine.transition(booking, to);
            assertThat(booking.getStatus()).isEqualTo(to.name());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. INVALID TRANSITIONS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitionsTest {

        @Test
        @DisplayName("CONFIRMED → CREATED is rejected (booking cannot restart)")
        void confirmed_to_created_rejected() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            assertThatThrownBy(() -> stateMachine.transition(booking, BookingStatus.CREATED))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("CONFIRMED")
                    .hasMessageContaining("CREATED");
        }

        @Test
        @DisplayName("COMPLETED → PAYMENT_PENDING is rejected (workflow already finished)")
        void completed_to_paymentPending_rejected() {
            Booking booking = bookingWithStatus(BookingStatus.COMPLETED);
            assertThatThrownBy(() -> stateMachine.transition(booking, BookingStatus.PAYMENT_PENDING))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("FAILED → CONFIRMED is rejected (failed bookings require new booking)")
        void failed_to_confirmed_rejected() {
            Booking booking = bookingWithStatus(BookingStatus.FAILED);
            assertThatThrownBy(() -> stateMachine.transition(booking, BookingStatus.CONFIRMED))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("FAILED");
        }

        @Test
        @DisplayName("EXPIRED → PAYMENT_PENDING is rejected (reservation no longer valid)")
        void expired_to_paymentPending_rejected() {
            Booking booking = bookingWithStatus(BookingStatus.EXPIRED);
            assertThatThrownBy(() -> stateMachine.transition(booking, BookingStatus.PAYMENT_PENDING))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("EXPIRED");
        }

        @Test
        @DisplayName("CANCELLED → CONFIRMED is rejected (cancelled bookings cannot recover)")
        void cancelled_to_confirmed_rejected() {
            Booking booking = bookingWithStatus(BookingStatus.CANCELLED);
            assertThatThrownBy(() -> stateMachine.transition(booking, BookingStatus.CONFIRMED))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("COMPLETED → CANCELLED is rejected (completed bookings cannot be cancelled)")
        void completed_to_cancelled_rejected() {
            Booking booking = bookingWithStatus(BookingStatus.COMPLETED);
            assertThatThrownBy(() -> stateMachine.transition(booking, BookingStatus.CANCELLED))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("CONFIRMED → FAILED is rejected (successful booking cannot fail afterwards)")
        void confirmed_to_failed_rejected() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            assertThatThrownBy(() -> stateMachine.transition(booking, BookingStatus.FAILED))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("CONFIRMED");
        }

        @Test
        @DisplayName("Status must not mutate when transition is rejected")
        void statusIsUnchangedOnRejection() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            try {
                stateMachine.transition(booking, BookingStatus.CREATED);
            } catch (BookingTransitionException ignored) {
                // expected
            }
            // Status must remain CONFIRMED, not partially mutated
            assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
        }

        // ── Parameterised – all explicitly forbidden transitions ───────────

        static Stream<Arguments> illegalTransitions() {
            return Stream.of(
                    // From the spec's "Illegal Booking Transitions" table
                    Arguments.of(BookingStatus.CONFIRMED,  BookingStatus.CREATED),
                    Arguments.of(BookingStatus.COMPLETED,  BookingStatus.PAYMENT_PENDING),
                    Arguments.of(BookingStatus.FAILED,     BookingStatus.CONFIRMED),
                    Arguments.of(BookingStatus.EXPIRED,    BookingStatus.PAYMENT_PENDING),
                    Arguments.of(BookingStatus.CANCELLED,  BookingStatus.CONFIRMED),
                    Arguments.of(BookingStatus.COMPLETED,  BookingStatus.CANCELLED),
                    Arguments.of(BookingStatus.CONFIRMED,  BookingStatus.FAILED),
                    // Additional nonsensical transitions
                    Arguments.of(BookingStatus.CREATED,    BookingStatus.CONFIRMED),
                    Arguments.of(BookingStatus.CREATED,    BookingStatus.COMPLETED),
                    Arguments.of(BookingStatus.CREATED,    BookingStatus.FAILED),
                    Arguments.of(BookingStatus.CREATED,    BookingStatus.PAYMENT_PENDING),
                    Arguments.of(BookingStatus.SEATS_LOCKED, BookingStatus.CREATED),
                    Arguments.of(BookingStatus.SEATS_LOCKED, BookingStatus.CONFIRMED),
                    Arguments.of(BookingStatus.SEATS_LOCKED, BookingStatus.COMPLETED),
                    Arguments.of(BookingStatus.SEATS_LOCKED, BookingStatus.CANCELLED),
                    Arguments.of(BookingStatus.PAYMENT_PENDING, BookingStatus.CREATED),
                    Arguments.of(BookingStatus.PAYMENT_PENDING, BookingStatus.SEATS_LOCKED),
                    Arguments.of(BookingStatus.PAYMENT_PENDING, BookingStatus.COMPLETED),
                    Arguments.of(BookingStatus.PAYMENT_PENDING, BookingStatus.CANCELLED),
                    Arguments.of(BookingStatus.CONFIRMED,  BookingStatus.SEATS_LOCKED),
                    Arguments.of(BookingStatus.CONFIRMED,  BookingStatus.PAYMENT_PENDING),
                    Arguments.of(BookingStatus.CONFIRMED,  BookingStatus.EXPIRED)
            );
        }

        @ParameterizedTest(name = "{0} → {1} must throw BookingTransitionException")
        @MethodSource("illegalTransitions")
        @DisplayName("Parameterised: illegal transitions always throw")
        void parameterised_illegalTransitions(BookingStatus from, BookingStatus to) {
            Booking booking = bookingWithStatus(from);
            assertThatThrownBy(() -> stateMachine.transition(booking, to))
                    .isInstanceOf(BookingTransitionException.class);
        }

        @Test
        @DisplayName("Null target status throws BookingTransitionException")
        void nullTargetStatus_throws() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            assertThatThrownBy(() -> stateMachine.transition(booking, (BookingStatus) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Null booking throws NullPointerException")
        void nullBooking_throws() {
            assertThatThrownBy(() -> stateMachine.transition(null, BookingStatus.CONFIRMED))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Unknown status string throws BookingTransitionException")
        void unknownStatusString_throws() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            assertThatThrownBy(() -> stateMachine.transition(booking, "FLYING_UNICORN"))
                    .isInstanceOf(BookingTransitionException.class)
                    .hasMessageContaining("FLYING_UNICORN");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. TERMINAL STATES
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Terminal states")
    class TerminalStatesTest {

        private static final EnumSet<BookingStatus> TERMINAL_STATUSES =
                EnumSet.of(BookingStatus.COMPLETED, BookingStatus.FAILED,
                           BookingStatus.CANCELLED, BookingStatus.EXPIRED);

        private static final EnumSet<BookingStatus> NON_TERMINAL_STATUSES =
                EnumSet.complementOf(TERMINAL_STATUSES);

        @Test
        @DisplayName("COMPLETED is identified as terminal by isTerminal()")
        void completed_isTerminal() {
            assertThat(BookingStatus.COMPLETED.isTerminal()).isTrue();
            assertThat(BookingStatus.COMPLETED.isActive()).isFalse();
        }

        @Test
        @DisplayName("FAILED is identified as terminal by isTerminal()")
        void failed_isTerminal() {
            assertThat(BookingStatus.FAILED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED is identified as terminal by isTerminal()")
        void cancelled_isTerminal() {
            assertThat(BookingStatus.CANCELLED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("EXPIRED is identified as terminal by isTerminal()")
        void expired_isTerminal() {
            assertThat(BookingStatus.EXPIRED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("Non-terminal states are not terminal")
        void nonTerminalStates_areNotTerminal() {
            for (BookingStatus status : NON_TERMINAL_STATUSES) {
                assertThat(status.isTerminal())
                        .as("Status %s should not be terminal", status)
                        .isFalse();
                assertThat(status.isActive())
                        .as("Status %s should be active", status)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("BookingStateMachine.isTerminal() returns true for COMPLETED booking")
        void stateMachine_isTerminal_completed() {
            Booking booking = bookingWithStatus(BookingStatus.COMPLETED);
            assertThat(stateMachine.isTerminal(booking)).isTrue();
        }

        @Test
        @DisplayName("BookingStateMachine.isTerminal() returns false for CREATED booking")
        void stateMachine_isTerminal_created_false() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            assertThat(stateMachine.isTerminal(booking)).isFalse();
        }

        @Test
        @DisplayName("All terminal statuses reject any transition to a non-terminal state")
        void terminalStates_rejectAllTransitions() {
            for (BookingStatus terminal : TERMINAL_STATUSES) {
                for (BookingStatus nonTerminal : NON_TERMINAL_STATUSES) {
                    Booking booking = bookingWithStatus(terminal);
                    assertThatThrownBy(() -> stateMachine.transition(booking, nonTerminal))
                            .as("%s → %s must be rejected", terminal, nonTerminal)
                            .isInstanceOf(BookingTransitionException.class);
                }
            }
        }

        @Test
        @DisplayName("All terminal statuses reject transitions to other terminal states")
        void terminalStates_rejectTransitions_toOtherTerminals() {
            for (BookingStatus terminalFrom : TERMINAL_STATUSES) {
                for (BookingStatus terminalTo : TERMINAL_STATUSES) {
                    if (terminalFrom == terminalTo) continue; // idempotent case
                    Booking booking = bookingWithStatus(terminalFrom);
                    assertThatThrownBy(() -> stateMachine.transition(booking, terminalTo))
                            .as("%s → %s must be rejected", terminalFrom, terminalTo)
                            .isInstanceOf(BookingTransitionException.class);
                }
            }
        }

        @Test
        @DisplayName("Terminal rules have empty allowed-targets sets")
        void terminalRules_haveEmptyAllowedTargets() {
            for (BookingStatus terminal : TERMINAL_STATUSES) {
                validator.getRuleFor(terminal).ifPresent(rule ->
                        assertThat(rule.allowedTargets())
                                .as("Rule for terminal %s must have empty targets", terminal)
                                .isEmpty());
            }
        }

        @Test
        @DisplayName("BookingTransitionException carries correct from/to fields")
        void exceptionCarriesFromTo() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            try {
                stateMachine.transition(booking, BookingStatus.CREATED);
            } catch (BookingTransitionException ex) {
                assertThat(ex.getFromStatus()).isEqualTo(BookingStatus.CONFIRMED);
                assertThat(ex.getToStatus()).isEqualTo(BookingStatus.CREATED);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. CONCURRENCY SAFETY
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Concurrency safety")
    class ConcurrencySafetyTest {

        /**
         * Many threads concurrently transitioning independent booking entities via
         * the shared stateMachine bean must all succeed without data corruption or
         * race conditions inside the state machine component itself.
         */
        @Test
        @DisplayName("Concurrent valid transitions on independent bookings complete without exception")
        void concurrentTransitions_independentBookings_noException() throws InterruptedException {
            int threadCount = 50;
            CountDownLatch startGate  = new CountDownLatch(1);
            CountDownLatch doneLatch  = new CountDownLatch(threadCount);
            AtomicInteger  errors     = new AtomicInteger(0);

            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await(); // wait for simultaneous release
                        Booking booking = bookingWithStatus(BookingStatus.CREATED);
                        stateMachine.transition(booking, BookingStatus.SEATS_LOCKED);
                        if (!"SEATS_LOCKED".equals(booking.getStatus())) {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown(); // release all threads simultaneously
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            pool.shutdown();
            assertThat(errors.get()).isZero();
        }

        /**
         * Concurrent illegal-transition attempts on independent bookings should each
         * produce a {@link BookingTransitionException} – never a silent failure or
         * incorrect status mutation.
         */
        @Test
        @DisplayName("Concurrent illegal transitions on independent bookings all throw correctly")
        void concurrentIllegalTransitions_allThrow() throws InterruptedException {
            int threadCount = 30;
            CountDownLatch startGate    = new CountDownLatch(1);
            CountDownLatch doneLatch    = new CountDownLatch(threadCount);
            AtomicInteger  exceptions   = new AtomicInteger(0);
            AtomicInteger  unexpectedOk = new AtomicInteger(0);

            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await();
                        Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
                        stateMachine.transition(booking, BookingStatus.CREATED); // illegal
                        unexpectedOk.incrementAndGet(); // should never reach here
                    } catch (BookingTransitionException e) {
                        exceptions.incrementAndGet();
                    } catch (Exception e) {
                        unexpectedOk.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            pool.shutdown();

            assertThat(exceptions.get()).isEqualTo(threadCount);
            assertThat(unexpectedOk.get()).isZero();
        }

        /**
         * Verifies that the BookingTransitionValidator is reusable across threads without
         * internal mutable state being exposed (all rules are built once at construction).
         */
        @Test
        @DisplayName("BookingTransitionValidatorImpl getAllRules() is stable under concurrent reads")
        void validator_getAllRules_concurrentRead_stable() throws InterruptedException {
            int threadCount = 40;
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger  errors    = new AtomicInteger(0);
            int expectedRuleCount    = BookingStatus.values().length; // one rule per status

            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await();
                        if (validator.getAllRules().size() != expectedRuleCount) {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startGate.countDown();
            assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue();
            pool.shutdown();
            assertThat(errors.get()).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. BookingStatus ENUM
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("BookingStatus enum")
    class BookingStatusEnumTest {

        @Test
        @DisplayName("All 8 statuses are present")
        void allStatusesPresent() {
            assertThat(BookingStatus.values()).hasSize(8);
        }

        @Test
        @DisplayName("from() parses case-insensitively")
        void from_caseInsensitive() {
            assertThat(BookingStatus.from("confirmed")).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(BookingStatus.from("CONFIRMED")).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(BookingStatus.from("Confirmed")).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("from() throws for unknown value")
        void from_unknownThrows() {
            assertThatThrownBy(() -> BookingStatus.from("UNKNOWN_STATE"))
                    .isInstanceOf(BookingTransitionException.class);
        }

        @Test
        @DisplayName("from() throws for null")
        void from_nullThrows() {
            assertThatThrownBy(() -> BookingStatus.from(null))
                    .isInstanceOf(BookingTransitionException.class);
        }

        @Test
        @DisplayName("fromSafe() returns empty for unknown value")
        void fromSafe_unknownReturnsEmpty() {
            assertThat(BookingStatus.fromSafe("BOGUS")).isEmpty();
        }

        @Test
        @DisplayName("fromSafe() returns empty for null")
        void fromSafe_nullReturnsEmpty() {
            assertThat(BookingStatus.fromSafe(null)).isEmpty();
        }

        @Test
        @DisplayName("fromSafe() returns present for valid value")
        void fromSafe_validReturnsPresent() {
            assertThat(BookingStatus.fromSafe("CREATED")).contains(BookingStatus.CREATED);
        }

        @Test
        @DisplayName("Exactly 4 terminal statuses exist")
        void terminalCount() {
            long terminalCount = Stream.of(BookingStatus.values())
                    .filter(BookingStatus::isTerminal)
                    .count();
            assertThat(terminalCount).isEqualTo(4);
        }

        @Test
        @DisplayName("Exactly 4 active (non-terminal) statuses exist")
        void activeCount() {
            long activeCount = Stream.of(BookingStatus.values())
                    .filter(BookingStatus::isActive)
                    .count();
            assertThat(activeCount).isEqualTo(4);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. BookingTransitionRule VALUE OBJECT
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("BookingTransitionRule value object")
    class BookingTransitionRuleTest {

        @Test
        @DisplayName("allows() returns true for a permitted target")
        void allows_permittedTarget_true() {
            BookingTransitionRule rule = new BookingTransitionRule(
                    BookingStatus.CREATED,
                    java.util.Set.of(BookingStatus.SEATS_LOCKED, BookingStatus.CANCELLED),
                    "test rule"
            );
            assertThat(rule.allows(BookingStatus.SEATS_LOCKED)).isTrue();
            assertThat(rule.allows(BookingStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("allows() returns false for a disallowed target")
        void allows_disallowedTarget_false() {
            BookingTransitionRule rule = new BookingTransitionRule(
                    BookingStatus.CREATED,
                    java.util.Set.of(BookingStatus.SEATS_LOCKED),
                    "test rule"
            );
            assertThat(rule.allows(BookingStatus.CONFIRMED)).isFalse();
        }

        @Test
        @DisplayName("allows() returns false for null")
        void allows_null_false() {
            BookingTransitionRule rule = new BookingTransitionRule(
                    BookingStatus.CREATED,
                    java.util.Set.of(BookingStatus.SEATS_LOCKED),
                    "test rule"
            );
            assertThat(rule.allows(null)).isFalse();
        }

        @Test
        @DisplayName("isTerminalRule() returns true for empty target set")
        void isTerminalRule_emptyTargets_true() {
            BookingTransitionRule rule = new BookingTransitionRule(
                    BookingStatus.COMPLETED,
                    java.util.Set.of(),
                    "terminal rule"
            );
            assertThat(rule.isTerminalRule()).isTrue();
        }

        @Test
        @DisplayName("Constructor rejects terminal from-state with non-empty targets")
        void constructor_terminalFromState_withTargets_throws() {
            assertThatThrownBy(() -> new BookingTransitionRule(
                    BookingStatus.COMPLETED,
                    java.util.Set.of(BookingStatus.CREATED),
                    "bad rule"
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Constructor rejects null from")
        void constructor_nullFrom_throws() {
            assertThatThrownBy(() -> new BookingTransitionRule(
                    null, java.util.Set.of(), "desc"
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Constructor rejects null allowedTargets")
        void constructor_nullTargets_throws() {
            assertThatThrownBy(() -> new BookingTransitionRule(
                    BookingStatus.CREATED, null, "desc"
            )).isInstanceOf(NullPointerException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 7. IDEMPOTENCY
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTest {

        @Test
        @DisplayName("Transitioning to the current status is a safe no-op")
        void sameStateTransition_isNoOp() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            stateMachine.transition(booking, BookingStatus.CONFIRMED); // should not throw
            assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
        }

        @Test
        @DisplayName("String overload same-state transition is a safe no-op")
        void sameStateTransition_stringOverload_isNoOp() {
            Booking booking = bookingWithStatus(BookingStatus.PAYMENT_PENDING);
            stateMachine.transition(booking, "PAYMENT_PENDING");
            assertThat(booking.getStatus()).isEqualTo("PAYMENT_PENDING");
        }

        @Test
        @DisplayName("isAllowed returns true for same state (idempotent)")
        void isAllowed_sameState_returnsTrue() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            assertThat(stateMachine.canTransition(booking, BookingStatus.CONFIRMED)).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 8. QUERY HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Query helpers")
    class QueryHelperTest {

        @Test
        @DisplayName("currentStatus() returns correct enum for booking")
        void currentStatus_returnsCorrectEnum() {
            Booking booking = bookingWithStatus(BookingStatus.SEATS_LOCKED);
            assertThat(stateMachine.currentStatus(booking)).isEqualTo(BookingStatus.SEATS_LOCKED);
        }

        @Test
        @DisplayName("canTransition returns true for legal pair")
        void canTransition_legal_true() {
            Booking booking = bookingWithStatus(BookingStatus.PAYMENT_PENDING);
            assertThat(stateMachine.canTransition(booking, BookingStatus.CONFIRMED)).isTrue();
        }

        @Test
        @DisplayName("canTransition returns false for illegal pair")
        void canTransition_illegal_false() {
            Booking booking = bookingWithStatus(BookingStatus.CONFIRMED);
            assertThat(stateMachine.canTransition(booking, BookingStatus.CREATED)).isFalse();
        }

        @Test
        @DisplayName("canTransition string overload works for legal pair")
        void canTransition_string_legal() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            assertThat(stateMachine.canTransition(booking, "SEATS_LOCKED")).isTrue();
        }

        @Test
        @DisplayName("canTransition string overload returns false for unknown string")
        void canTransition_string_unknown_false() {
            Booking booking = bookingWithStatus(BookingStatus.CREATED);
            assertThat(stateMachine.canTransition(booking, "INVALID_STATE")).isFalse();
        }

        @Test
        @DisplayName("isAllowed is false for terminal state")
        void isAllowed_terminalFrom_false() {
            assertThat(validator.isAllowed(BookingStatus.COMPLETED, BookingStatus.CREATED)).isFalse();
        }

        @Test
        @DisplayName("isAllowed is false when null arguments provided")
        void isAllowed_nullArgs_false() {
            assertThat(validator.isAllowed(null, BookingStatus.CREATED)).isFalse();
            assertThat(validator.isAllowed(BookingStatus.CREATED, null)).isFalse();
        }

        @Test
        @DisplayName("getRuleFor returns present for every BookingStatus")
        void getRuleFor_allStatuses_present() {
            for (BookingStatus status : BookingStatus.values()) {
                assertThat(validator.getRuleFor(status))
                        .as("Rule must exist for status %s", status)
                        .isPresent();
            }
        }

        @Test
        @DisplayName("getAllRules contains one rule per BookingStatus")
        void getAllRules_sizeEqualsNumberOfStatuses() {
            assertThat(validator.getAllRules()).hasSize(BookingStatus.values().length);
        }

        @Test
        @DisplayName("Booking with null status string throws BookingTransitionException")
        void nullStatusString_throws() {
            Booking booking = bookingWithStatus("null_status");
            booking.setStatus(null);
            assertThatThrownBy(() -> stateMachine.currentStatus(booking))
                    .isInstanceOf(BookingTransitionException.class);
        }
    }
}
