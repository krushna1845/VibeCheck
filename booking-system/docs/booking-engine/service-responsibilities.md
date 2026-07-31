# Booking Engine - Service Responsibilities

**Version:** 1.0  
**Author:** Krushna Malode  
**Status:** Draft  
**Last Updated:** 2026-07-29

---

# 1. Purpose

This document defines the internal architecture of the Booking Engine by assigning clear responsibilities to each service and component.

The objective is to achieve a clean, maintainable, scalable, and loosely coupled architecture where every class has a single responsibility.

The Booking Engine follows the Single Responsibility Principle (SRP), Separation of Concerns (SoC), and Clean Architecture principles.

---

# 2. Design Principles

The Booking Engine follows the following architectural principles:

- Single Responsibility Principle
- Separation of Concerns
- Dependency Injection
- Stateless Service Design
- Transactional Consistency
- Event-Driven Communication
- High Cohesion
- Low Coupling
- Interface-Based Design
- Testability

---

# 3. High-Level Architecture

```
                +-------------------+
                |   Booking API     |
                +---------+---------+
                          |
                          |
                          v
                +-------------------+
                |  BookingService   |
                +---------+---------+
                          |
        +-----------------+------------------+
        |                 |                  |
        v                 v                  v
+---------------+ +----------------+ +--------------------+
| BookingValidator | SeatLockService | BookingStateMachine |
+---------------+ +----------------+ +--------------------+
        |                 |                  |
        |                 |                  |
        v                 |                  |
 Booking Repository        |                  |
                           |                  |
                           v                  |
                     Redis Repository         |
                                              |
                                              |
                         +---------------------+
                         |
                         v
                 BookingEventPublisher
                         |
                         v
                       Kafka
```

---


# 4. Core Components

## 4.1 BookingService

### Responsibility

BookingService acts as the orchestration layer.

It coordinates the complete booking workflow but does not contain validation, Redis operations, Kafka publishing, or payment implementation.

### Responsibilities

- Receive booking requests
- Coordinate booking workflow
- Invoke validators
- Acquire seat locks
- Create booking
- Initiate payment
- Confirm booking
- Cancel booking
- Publish booking events

### Should NOT

- Access Redis directly
- Send emails
- Validate business rules
- Implement payment gateway logic
- Build Kafka messages manually

---

## 4.2 BookingValidator

### Responsibility

Validates all business rules before booking creation.

### Responsibilities

- Validate user
- Validate show
- Validate seats
- Validate booking ownership
- Validate booking state
- Validate booking request

### Should NOT

- Save data
- Publish events
- Call payment gateway

---

## 4.3 SeatLockService

### Responsibility

Manages temporary seat reservations.

### Responsibilities

- Lock seats
- Unlock seats
- Extend lock timeout
- Validate lock ownership
- Detect expired locks

### Technology

Redis

### Should NOT

- Save bookings
- Publish events
- Process payments

---

## 4.4 BookingStateMachine

### Responsibility

Controls all booking state transitions.

### Responsibilities

- Validate transitions
- Prevent illegal transitions
- Enforce booking lifecycle
- Maintain workflow consistency

### Should NOT

- Query database
- Call Redis
- Publish Kafka events

---

## 4.5 PaymentService

### Responsibility

Coordinates payment processing.

### Responsibilities

- Create payment request
- Validate callback
- Update payment status
- Notify BookingService

### Should NOT

- Confirm bookings directly
- Lock seats
- Send notifications

---

## 4.6 BookingEventPublisher

### Responsibility

Publishes domain events.

### Responsibilities

- Publish BookingCreatedEvent
- Publish BookingConfirmedEvent
- Publish BookingCancelledEvent
- Publish BookingExpiredEvent

### Technology

Kafka

---

## 4.7 BookingScheduler

### Responsibility

Background cleanup tasks.

### Responsibilities

- Detect expired bookings
- Release expired locks
- Update booking status
- Publish expiration events

### Execution

Every 60 seconds.

---

## 4.8 Notification Service

### Responsibility

Handles customer communication.

### Responsibilities

- Email confirmation
- Booking cancellation notification
- Booking expiration notification

Runs asynchronously.

---

# 5. Repository Responsibilities

## BookingRepository

Responsibilities

- CRUD Booking
- Find by Booking Reference
- Find by Status
- Find Expired Bookings
- Find by Customer

---

## BookingSeatRepository

Responsibilities

- Save Booking Seats
- Retrieve Booking Seats
- Find Seats by Booking

---

## PaymentRepository

Responsibilities

- Store payment
- Retrieve payment
- Update payment status

---

# 6. External Integrations

## Redis

Used for

- Seat locking
- Lock expiration
- Lock ownership

Redis never stores permanent booking information.

---

## Kafka

Used for

- Booking events
- Payment events
- Notification events

Kafka is asynchronous.

---

## Payment Gateway

Used for

- Payment authorization
- Payment confirmation
- Payment failure notification

---

# 7. Dependency Flow

```
Controller

↓

BookingService

↓

BookingValidator

↓

SeatLockService

↓

BookingRepository

↓

PaymentService

↓

BookingEventPublisher
```

Dependencies always point downward.

Lower layers must never depend on higher layers.

---

# 8. Component Interaction Matrix

| Component | Calls |
|------------|------|
| BookingController | BookingService |
| BookingService | BookingValidator |
| BookingService | SeatLockService |
| BookingService | BookingRepository |
| BookingService | PaymentService |
| BookingService | BookingStateMachine |
| BookingService | BookingEventPublisher |
| BookingScheduler | BookingRepository |
| BookingScheduler | SeatLockService |
| PaymentService | Payment Gateway |
| BookingEventPublisher | Kafka |

---

# 9. Transaction Ownership

| Component | Owns Transaction |
|------------|-----------------|
| BookingService | Yes |
| BookingValidator | No |
| SeatLockService | No |
| BookingStateMachine | No |
| BookingEventPublisher | No |
| Notification Service | No |

BookingService is the only component responsible for coordinating transactional operations.

---

# 10. Logging Responsibilities

Each component logs only its own responsibilities.

BookingService

- Booking creation
- Booking confirmation
- Booking cancellation

BookingValidator

- Validation failures

SeatLockService

- Lock acquired
- Lock released
- Lock timeout

PaymentService

- Payment initiated
- Payment callback
- Payment failure

BookingScheduler

- Booking expiration
- Lock cleanup

BookingEventPublisher

- Published events

---

# 11. Error Handling Responsibilities

BookingService

- Business exceptions

BookingValidator

- Validation exceptions

SeatLockService

- Lock exceptions

PaymentService

- Payment exceptions

BookingScheduler

- Recovery exceptions

Every component throws domain-specific exceptions.

---

# 12. Design Decisions

DD-001

BookingService acts only as an orchestrator.

---

DD-002

Business validation is isolated inside BookingValidator.

---

DD-003

Redis logic is isolated inside SeatLockService.

---

DD-004

State transitions are centralized inside BookingStateMachine.

---

DD-005

Kafka publishing is isolated inside BookingEventPublisher.

---

DD-006

Schedulers are responsible only for cleanup tasks.

---

DD-007

Each component owns exactly one business responsibility.

---

# 13. Summary

The Booking Engine architecture is designed around small, focused components that collaborate through well-defined interfaces.

This architecture provides:

- High maintainability
- Easy testing
- Low coupling
- High cohesion
- Better scalability
- Clear ownership of business logic
- Simpler debugging
- Easier future enhancements

Every component has a single responsibility, making the Booking Engine easier to understand, extend, and maintain.