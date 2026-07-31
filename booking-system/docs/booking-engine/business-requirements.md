# Booking Engine - Business Requirements

Version: 1.0

Author: Krushna Malode

Status: Draft

Last Updated: 2026-07-29

---

# 1. Purpose

## 1.1 Objective

The Booking Engine is the core transactional component of the Movie Booking Platform. It is responsible for securely reserving seats, managing booking lifecycles, coordinating payment initiation, and ensuring seat inventory remains consistent under concurrent access.

The Booking Engine guarantees that a seat can never be confirmed for more than one customer while providing a reliable and scalable booking experience.

The service coordinates booking creation, seat reservation, booking confirmation, booking cancellation, booking expiration, and communication with external services such as Payment, Notification, and API Gateway.

---

## 1.2 Goals

The Booking Engine is designed to achieve the following goals:

- Prevent double booking of seats.
- Maintain strong consistency for confirmed bookings.
- Support high concurrent booking requests.
- Minimize seat lock duration.
- Provide reliable booking recovery after failures.
- Support asynchronous communication with other services.
- Maintain a complete audit trail of booking state changes.

---

# 2. Scope

## 2.1 In Scope

The Booking Engine is responsible for:

- Creating bookings
- Reserving seats
- Releasing expired seat reservations
- Confirming bookings after successful payment
- Cancelling bookings
- Maintaining booking history
- Managing booking state transitions
- Publishing booking events
- Integrating with Redis for temporary seat locks
- Integrating with Payment Service
- Integrating with Notification Service
- Coordinating booking-related workflows

---

## 2.2 Out of Scope

The Booking Engine does NOT perform the following responsibilities:

- User authentication
- User authorization
- Movie management
- Theatre management
- Show scheduling
- Payment gateway implementation
- Email delivery
- SMS delivery
- Push notification delivery
- Analytics
- Recommendation engine

These responsibilities belong to other dedicated microservices.

---

# 3. Stakeholders

The following systems interact with the Booking Engine.

## Customer

Responsibilities

- Browse available seats
- Reserve seats
- Pay for bookings
- View booking history
- Cancel eligible bookings

---

## Administrator

Responsibilities

- View booking information
- Cancel bookings
- Resolve customer issues

---

## API Gateway

Responsibilities

- Route booking requests
- Authenticate users
- Forward validated requests

---

## Payment Service

Responsibilities

- Initiate payment
- Validate payment result
- Notify booking service of payment completion

---

## Notification Service

Responsibilities

- Send booking confirmation
- Send cancellation notification
- Send booking expiry notification

---

## Redis

Responsibilities

- Temporary seat locking
- Lock expiration
- Lock ownership verification

---

## Kafka

Responsibilities

- Publish booking events
- Deliver booking events
- Enable asynchronous communication

---

# 4. Functional Requirements

## Booking Management

FR-001

The system shall allow an authenticated customer to create a booking.

FR-002

The system shall allow booking of one or more seats for a specific show.

FR-003

The system shall validate that all selected seats belong to the requested show.

FR-004

The system shall validate seat availability before creating a booking.

FR-005

The system shall temporarily reserve seats before payment.

FR-006

The system shall automatically expire inactive reservations after the configured timeout.

FR-007

The system shall release all expired seat reservations.

FR-008

The system shall confirm bookings only after successful payment.

FR-009

The system shall cancel bookings when payment fails.

FR-010

The system shall allow customers to retrieve booking history.

FR-011

The system shall allow customers to view booking details.

FR-012

The system shall generate a unique booking reference for every booking.

FR-013

The system shall publish booking events after successful booking confirmation.

FR-014

The system shall publish booking cancellation events.

FR-015

The system shall support booking status tracking.

FR-016

The system shall maintain complete booking audit information.

FR-017

The system shall prevent duplicate booking confirmation.

FR-018

The system shall reject invalid booking state transitions.

---

# 5. Non-Functional Requirements

## Performance

NFR-001

Seat reservation should complete within acceptable response times under normal load.

NFR-002

The system should efficiently support large numbers of concurrent booking requests.

---

## Reliability

NFR-003

No confirmed booking shall lose ownership of reserved seats.

NFR-004

Booking operations shall recover safely from unexpected failures.

---

## Scalability

NFR-005

The Booking Engine shall support horizontal scaling.

---

## Availability

NFR-006

Temporary failures in downstream services shall not permanently corrupt booking data.

---

## Security

NFR-007

Only authenticated users may create bookings.

NFR-008

Booking ownership shall be validated before allowing cancellation or retrieval.

---

## Auditability

NFR-009

Every booking state transition shall be logged.

NFR-010

All booking operations shall be traceable using correlation identifiers.

---

# 6. Business Rules

BR-001

A seat may belong to only one confirmed booking.

BR-002

Only AVAILABLE seats may be reserved.

BR-003

Only LOCKED seats may transition to BOOKED.

BR-004

Seat locks expire automatically after the configured timeout.

BR-005

Expired bookings release all associated seat locks.

BR-006

Booking confirmation requires successful payment.

BR-007

Cancelled bookings cannot be confirmed.

BR-008

Confirmed bookings cannot return to pending states.

BR-009

A booking must contain at least one seat.

BR-010

All seats within a booking must belong to the same show.

BR-011

Booking references must be globally unique.

BR-012

Every booking state transition must be validated by the Booking State Machine.

---

# 7. Assumptions

The Booking Engine assumes:

- User authentication is already completed by the Authentication Service.
- Authorization is enforced before requests reach the Booking Engine.
- Movie, Theatre, and Show data are maintained by their respective services.
- Payment callbacks are eventually delivered.
- Redis is available for temporary seat locking.
- Kafka is available for asynchronous event publishing.
- System clocks are reasonably synchronized across services.

---

# 8. Future Enhancements

The Booking Engine is designed to support future capabilities including:

- Dynamic seat pricing
- Coupon and promotional discounts
- Loyalty reward integration
- Group booking support
- Seat recommendations
- Waitlist management
- Partial ticket cancellation
- Partial refunds
- Multi-currency payments
- Multi-language booking confirmation
- Booking analytics dashboard

---


# 9. Success Criteria

The Booking Engine will be considered successful when:

- No seat can be sold twice.
- Seat reservations automatically expire.
- Booking confirmation occurs only after successful payment.
- The system remains stable under concurrent booking requests.
- Booking history remains accurate and auditable.
- Every booking state transition follows the defined state machine.
- External service failures do not permanently corrupt booking data.


