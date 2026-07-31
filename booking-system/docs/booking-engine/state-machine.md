# Booking Engine - State Machine

**Version:** 1.0  
**Author:** Krushna Malode  
**Status:** Draft  
**Last Updated:** 2026-07-29

---

# 1. Purpose

This document defines the lifecycle of Booking, ShowSeat, and Payment within the Movie Booking Platform.

The purpose of the Booking State Machine is to ensure that every entity transitions only through valid states while preventing illegal operations that could lead to inconsistent data, double booking, or invalid payment processing.

This document serves as the source of truth for all state transitions implemented in the Booking Engine.

---

# 2. Objectives

The Booking State Machine is designed to achieve the following objectives:

- Prevent invalid booking transitions.
- Maintain consistency between Booking and ShowSeat.
- Coordinate booking and payment lifecycle.
- Support recovery after failures.
- Prevent duplicate booking confirmation.
- Ensure booking expiration is deterministic.
- Support event-driven architecture.

---

# 3. Booking Lifecycle

A booking progresses through multiple stages from creation to completion.

```text
                +----------------+
                |    CREATED     |
                +----------------+
                        |
                        |
                        v
               +------------------+
               |  SEATS_LOCKED    |
               +------------------+
                        |
                        |
                        v
            +------------------------+
            |   PAYMENT_PENDING      |
            +------------------------+
              |                  |
              |                  |
      Payment Success     Payment Failure
              |                  |
              v                  v
     +----------------+   +---------------+
     |   CONFIRMED    |   |    FAILED     |
     +----------------+   +---------------+
              |
              |
              v
      +----------------+
      |   COMPLETED    |
      +----------------+

Timeout
CREATED
   |
   v
EXPIRED

User Cancellation
CREATED
   |
   v
CANCELLED
```

---

# 4. Booking Status Definitions

## CREATED

Description

Booking record has been created.

Characteristics

- Booking Reference generated
- Seats not permanently booked
- Redis lock not yet confirmed
- Customer has not started payment

Allowed Transitions

- SEATS_LOCKED
- CANCELLED
- EXPIRED

---

## SEATS_LOCKED

Description

Requested seats have been successfully locked in Redis.

Characteristics

- Seats unavailable to other customers
- Lock has expiration time
- Booking is temporary

Allowed Transitions

- PAYMENT_PENDING
- FAILED
- EXPIRED

---

## PAYMENT_PENDING

Description

Customer is currently completing payment.

Characteristics

- Waiting for payment callback
- Redis locks remain active

Allowed Transitions

- CONFIRMED
- FAILED
- EXPIRED

---

## CONFIRMED

Description

Payment has been successfully verified.

Characteristics

- Booking becomes permanent
- Seats permanently reserved
- Confirmation event published

Allowed Transitions

- COMPLETED

---

## COMPLETED

Description

Entire booking workflow has finished successfully.

Characteristics

- Notification sent
- Audit log completed

Allowed Transitions

None

Terminal State

---

## FAILED

Description

Booking failed due to business or payment failure.

Characteristics

- Redis locks released
- Seats become available

Allowed Transitions

None

Terminal State

---

## CANCELLED

Description

Booking cancelled before confirmation.

Characteristics

- Seats released
- Booking closed

Allowed Transitions

None

Terminal State

---

## EXPIRED

Description

Customer failed to complete booking within lock timeout.

Characteristics

- Scheduler releases Redis locks
- Booking closed automatically

Allowed Transitions

None

Terminal State

---

# 5. Booking Transition Matrix

| Current State | Allowed Next States |
|---------------|---------------------|
| CREATED | SEATS_LOCKED, CANCELLED, EXPIRED |
| SEATS_LOCKED | PAYMENT_PENDING, FAILED, EXPIRED |
| PAYMENT_PENDING | CONFIRMED, FAILED, EXPIRED |
| CONFIRMED | COMPLETED |
| COMPLETED | None |
| FAILED | None |
| CANCELLED | None |
| EXPIRED | None |

---

# 6. Illegal Booking Transitions

The following transitions are forbidden.

| Invalid Transition | Reason |
|--------------------|--------|
| CONFIRMED → CREATED | Booking cannot restart |
| COMPLETED → PAYMENT_PENDING | Workflow already finished |
| FAILED → CONFIRMED | Failed bookings require new booking |
| EXPIRED → PAYMENT_PENDING | Reservation no longer valid |
| CANCELLED → CONFIRMED | Cancelled bookings cannot recover |
| COMPLETED → CANCELLED | Completed bookings cannot be cancelled |
| CONFIRMED → FAILED | Successful booking cannot fail afterwards |

Attempting any illegal transition must throw an InvalidBookingStateTransitionException.

---

# 7. ShowSeat Lifecycle

Each ShowSeat has its own independent lifecycle.

```text
AVAILABLE
     |
     |
     v
LOCKED
     |
     |
     +------------------+
     |                  |
     |                  |
     v                  v
BOOKED            AVAILABLE
              (Lock Released)
```

---

# 8. ShowSeat Status Definitions

## AVAILABLE

Seat can be selected.

Allowed Transition

- LOCKED

---

## LOCKED

Seat is temporarily reserved.

Allowed Transitions

- BOOKED
- AVAILABLE

---

## BOOKED

Seat permanently belongs to confirmed booking.

Allowed Transitions

None

Terminal State

---

## BLOCKED

Seat unavailable due to maintenance or operational reasons.

Allowed Transitions

None

Terminal State

---

# 9. ShowSeat Transition Matrix

| Current | Allowed |
|----------|----------|
| AVAILABLE | LOCKED |
| LOCKED | BOOKED, AVAILABLE |
| BOOKED | None |
| BLOCKED | None |

---

# 10. Payment Lifecycle

```text
PENDING
   |
   |
   +----------------+
   |                |
   |                |
   v                v
SUCCESS          FAILED
   |
   |
   v
REFUNDED
```

---

# 11. Payment Status Definitions

## PENDING

Payment initiated.

Allowed Transitions

- SUCCESS
- FAILED

---

## SUCCESS

Payment verified.

Allowed Transition

- REFUNDED

---

## FAILED

Payment unsuccessful.

Allowed Transitions

None

Terminal State

---

## REFUNDED

Payment refunded.

Allowed Transitions

None

Terminal State

---

# 12. Payment Transition Matrix

| Current | Allowed |
|----------|----------|
| PENDING | SUCCESS, FAILED |
| SUCCESS | REFUNDED |
| FAILED | None |
| REFUNDED | None |

---

# 13. Booking State Machine Rules

Rule 1

Only AVAILABLE seats may be locked.

---

Rule 2

Only LOCKED seats may become BOOKED.

---

Rule 3

Booking confirmation requires successful payment.

---

Rule 4

Booking expiration releases every locked seat.

---

Rule 5

Payment callback must be idempotent.

---

Rule 6

Duplicate booking confirmation must never occur.

---

Rule 7

A booking must contain at least one seat.

---

Rule 8

Every seat within a booking belongs to the same show.

---

Rule 9

Booking reference is immutable.

---

Rule 10

Terminal states cannot transition to any other state.

---

# 14. Scheduler Responsibilities

The Booking Scheduler periodically checks for expired bookings.

Responsibilities

- Detect expired bookings
- Release Redis seat locks
- Update booking status to EXPIRED
- Publish BookingExpiredEvent
- Write audit logs

Execution Interval

Every 60 seconds.

---

# 15. Event Mapping

| Event | Trigger |
|--------|----------|
| BookingCreatedEvent | Booking CREATED |
| SeatsLockedEvent | SEATS_LOCKED |
| PaymentInitiatedEvent | PAYMENT_PENDING |
| BookingConfirmedEvent | CONFIRMED |
| BookingCompletedEvent | COMPLETED |
| BookingFailedEvent | FAILED |
| BookingExpiredEvent | EXPIRED |
| BookingCancelledEvent | CANCELLED |

---

# 16. State Machine Validation

The Booking State Machine must validate every transition before persistence.

If a transition is invalid:

- Reject the request
- Roll back the transaction
- Log the violation
- Throw InvalidBookingStateTransitionException

No direct database update should bypass the state machine.

---

# 17. Design Principles

The Booking State Machine follows the following principles:

- Single Source of Truth
- Deterministic State Transitions
- Event-Driven Workflow
- Idempotent Operations
- Transactional Consistency
- Failure Recovery
- Immutable Booking History
- High Concurrency Support
- Explicit State Validation
- Auditability

---


# 18. Summary

The Booking State Machine provides a deterministic lifecycle for Booking, ShowSeat, and Payment entities.

It guarantees:

- Valid state transitions only.
- No double booking.
- Automatic reservation expiration.
- Safe payment processing.
- Consistent booking lifecycle.
- Predictable recovery after failures.
- Strong transactional integrity.

This document serves as the implementation blueprint for the BookingStateMachine component that will be developed in the Booking Engine.
