# Booking Engine - Failure Scenarios

**Version:** 1.0  
**Author:** Krushna Malode  
**Status:** Draft  
**Last Updated:** 2026-07-29

---

# 1. Purpose

This document identifies all major failure scenarios that may occur within the Booking Engine and defines the expected system behavior, recovery strategy, and architectural decisions for each case.

The objective is to ensure that failures never leave the system in an inconsistent state and that users experience predictable, recoverable behavior.

---

# 2. Objectives

The failure handling strategy aims to:

- Prevent data corruption.
- Prevent double booking.
- Recover automatically whenever possible.
- Keep transactions consistent.
- Provide meaningful error responses.
- Support high availability.
- Ensure idempotent operations.
- Maintain complete audit logs.

---

# 3. Failure Categories

The Booking Engine considers failures in the following categories:

- Validation Failures
- Concurrency Failures
- Database Failures
- Redis Failures
- Payment Failures
- Kafka Failures
- Scheduler Failures
- External Service Failures
- Network Failures
- Unexpected Runtime Failures

---

# 4. Validation Failures

## Scenario

Customer selects invalid seats.

Possible Causes

- Seat does not exist.
- Show does not exist.
- Seat belongs to another show.
- Invalid request.

Expected Behaviour

- Reject request.
- Return HTTP 400.
- Do not create booking.
- Do not lock seats.

Recovery

Customer corrects request and retries.

---

# 5. Seat Already Locked

## Scenario

Two customers select the same seat simultaneously.

Expected Behaviour

Customer A acquires Redis lock.

Customer B receives

HTTP 409 Conflict

Booking creation stops immediately.

Recovery

Customer B selects another seat.

---

# 6. Redis Lock Failure

## Scenario

Redis becomes unavailable.

Expected Behaviour

- Reject booking request.
- Return HTTP 503.
- Do not create booking.

Recovery

Retry after Redis becomes available.

Monitoring

Generate infrastructure alert.

---

# 7. Booking Creation Failure

## Scenario

Database insertion fails.

Expected Behaviour

- Roll back transaction.
- Release Redis locks.
- Return HTTP 500.

Recovery

Customer retries booking.

---

# 8. Payment Failure

## Scenario

Payment gateway rejects payment.

Expected Behaviour

- Booking status → FAILED
- Release Redis locks.
- Seats become AVAILABLE.

Recovery

Customer starts a new booking.

---

# 9. Payment Timeout

## Scenario

Customer closes payment page.

Expected Behaviour

Booking remains

PAYMENT_PENDING

Scheduler later changes it to

EXPIRED

Recovery

Customer creates a new booking.

---

# 10. Duplicate Payment Callback

## Scenario

Gateway sends callback twice.

Expected Behaviour

First callback

Booking confirmed.

Second callback

Ignored.

Recovery

No action required.

Implementation

Idempotency Key.

---

# 11. Kafka Failure

## Scenario

Booking confirmed.

Kafka unavailable.

Expected Behaviour

Booking remains confirmed.

No rollback occurs.

Recovery

Outbox Pattern retries event publication.

---

# 12. Notification Failure

## Scenario

Email service unavailable.

Expected Behaviour

Booking remains confirmed.

Notification retried asynchronously.

Recovery

Automatic retry.

---

# 13. Scheduler Failure

## Scenario

Expiration scheduler stops.

Expected Behaviour

Redis TTL still expires.

Scheduler resumes on restart.

Recovery

Restart scheduler.

Monitoring alert generated.

---

# 14. Database Failure

## Scenario

Database unavailable.

Expected Behaviour

Reject booking request.

Return HTTP 503.

Recovery

Automatic reconnection.

Infrastructure alert.

---

# 15. Network Failure

## Scenario

Temporary communication failure.

Examples

- Payment Service
- Kafka
- Redis

Expected Behaviour

Retry according to retry policy.

Recovery

Exponential backoff.

Maximum retry count enforced.

---

# 16. Service Crash During Booking

## Scenario

Booking Service crashes after booking creation.

Expected Behaviour

Booking remains

CREATED

Scheduler detects timeout.

Booking expires.

Redis locks released.

Recovery

Automatic.

---

# 17. Service Crash During Confirmation

## Scenario

Payment succeeds.

Booking Service crashes before confirmation.

Expected Behaviour

Payment callback retried.

Booking eventually confirmed.

Recovery

Idempotent callback processing.

---

# 18. Concurrent Booking Requests

## Scenario

Thousands of users attempt to reserve the same seats.

Expected Behaviour

Redis ensures only one successful lock.

Optimistic locking prevents database conflicts.

Recovery

Unsuccessful users retry.

---

# 19. Partial Failure Matrix

| Operation | Failure | Recovery |
|------------|----------|----------|
| Validation | Invalid request | Client correction |
| Redis Lock | Redis unavailable | Retry |
| Booking Insert | DB failure | Rollback |
| Payment | Payment failed | Release seats |
| Confirmation | Callback retry | Idempotency |
| Kafka Publish | Broker unavailable | Outbox retry |
| Notification | Email failure | Async retry |
| Scheduler | Scheduler stopped | Restart |
| Database | Connection lost | Retry |

---

# 20. Retry Strategy

| Component | Retry Strategy |
|------------|----------------|
| Payment Service | Exponential Backoff |
| Kafka Publisher | Outbox Retry |
| Notification | Retry Queue |
| Redis | Limited Retry |
| Database | Connection Pool Retry |

---

# 21. Monitoring Strategy

Critical metrics:

- Booking Success Rate
- Booking Failure Rate
- Payment Success Rate
- Payment Failure Rate
- Seat Lock Success Rate
- Redis Availability
- Kafka Availability
- Scheduler Health
- Average Booking Time
- Average Payment Time

---


# 22. Logging Strategy


Every failure log should include:

- Correlation ID
- Booking ID
- User ID
- Show ID
- Seat IDs
- Timestamp
- Exception Type
- Service Name
- Retry Count

Sensitive information (such as payment details) must never be logged.

---

# 23. Alerting Strategy

Critical Alerts

- Redis Down
- Kafka Down
- Database Down
- Payment Gateway Down
- Scheduler Stopped
- Booking Failure Rate Above Threshold

Warnings

- Notification Delay
- High Retry Count
- Slow Booking Response
- Elevated Lock Timeout

---

# 24. Recovery Principles

The Booking Engine follows these recovery principles:

- Fail Fast
- Retry Safe Operations
- Never Retry Invalid Requests
- Prefer Automatic Recovery
- Use Idempotency for Duplicate Requests
- Release Resources Quickly
- Log Every Failure
- Preserve Data Integrity
- Avoid Manual Intervention Whenever Possible

---

# 25. Design Decisions

## DD-001

Redis is the first line of defense against double booking.

---

## DD-002

Payment confirmation must be idempotent.

---

## DD-003

Booking confirmation is never rolled back because of notification failure.

---

## DD-004

Kafka publication failures are recovered asynchronously.

---

## DD-005

Database failures always trigger transaction rollback.

---

## DD-006

Expired bookings are recovered by the scheduler.

---

## DD-007

Failures should isolate individual bookings and never impact unrelated bookings.

---

# 26. Summary

The Booking Engine is designed to remain reliable under failures by combining short-lived transactions, Redis seat locking, optimistic concurrency control, asynchronous event publishing, idempotent payment processing, automatic retries, and scheduler-based recovery.

These strategies ensure that:

- No seat is confirmed twice.
- Temporary failures do not corrupt booking data.
- Users receive predictable responses.
- Infrastructure failures remain isolated.
- The platform can recover automatically without manual intervention in most scenarios.