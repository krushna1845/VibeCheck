# Milestone 33 — Payment Hardening & Verification Document

**Date:** 2026-09-29  
**Platform:** VibeCheck Movie Booking Platform  
**Target Milestone:** 33 — Production Application E2E Hardening  
**Scope:** Payment Initiation, Provider Integrations (Mock / Razorpay / Stripe), Webhooks, Idempotency, and Booking Confirmation  

---

## 1. End-to-End Payment Lifecycle Architecture

```
[ Frontend Client ]
       │
       ├── 1. POST /api/v1/payments
       │       Payload: { bookingId, userId, amount, currency: "INR", paymentMethod, bookingReference, idempotencyKey }
       │       Headers: Authorization: Bearer <JWT>, Idempotency-Key: <UUID>
       ▼
[ API Gateway (8079) ]
       │ (Forwards to payment-service:8085)
       ▼
[ payment-service ]
       │
       ├── 2. PaymentValidator checks positive amount, currency, reference
       ├── 3. Redis / DB Idempotency Check (returns cached response if duplicate key)
       ├── 4. Persists Payment entity with status = INITIATED
       ├── 5. Invokes PaymentClient (MOCK / Razorpay / Stripe)
       │       └── Returns: { paymentId, transactionReference, status: "INITIATED", redirectUrl }
       │
       ▼
[ Gateway / Provider Execution ]
       │
       ├── Case A: Webhook Delivery (POST /api/v1/payments/webhooks/{provider})
       │       └── Verifies HMAC-SHA256 signature against webhook secret
       │
       ├── Case B: Callback / Confirm Loop (POST /api/v1/bookings/confirm)
       │       Payload: { bookingReference, paymentId }
       │
       ▼
[ booking-service ]
       ├── 6. Verifies booking exists in PENDING status (not EXPIRED/CANCELLED)
       ├── 7. Calls show-service: updateShowSeatsStatus(showId, showSeatIds, "BOOKED")
       ├── 8. Mutates booking status to CONFIRMED
       ├── 9. Releases Redis seat locks (seat:{showId}:{seatId})
       ├── 10. Emits BookingConfirmedEvent to Kafka
       └── 11. Returns confirmed BookingResponse with HTTP 200 OK
```

---

## 2. 22-Point Payment Verification Matrix

| Check # | Verification Item | Mechanism / Implementation | Status |
|:---|:---|:---|:---:|
| **PAY-01** | Payment button calls correct backend endpoint | `web/js/app.js:processPayment()` invokes `POST /api/v1/payments` via `VibeCheckApi.payments.initiatePayment()` | **PASS** |
| **PAY-02** | Correct booking reference transmitted | Sent in `PaymentRequest.bookingReference` matching `state.currentBooking.bookingReference` | **PASS** |
| **PAY-03** | Correct total amount transmitted | Calculated from ticket seats + food + convenience fee, sent in `PaymentRequest.amount` | **PASS** |
| **PAY-04** | Payment order/transaction created | `PaymentServiceImpl:initiatePayment()` persists row in `vibecheck_payment.payments` | **PASS** |
| **PAY-05** | Frontend receives expected payment DTO | Receives `PaymentResponse` (`paymentId`, `transactionReference`, `status: INITIATED`) | **PASS** |
| **PAY-06** | Payment UI displays correctly | Step 3 displays detailed price breakdown, UPI/Card selectors, and Pay CTA | **PASS** |
| **PAY-07** | Success callback / confirmation works | `POST /api/v1/bookings/confirm` transitions booking from PENDING to CONFIRMED | **PASS** |
| **PAY-08** | Failure callback works | Gateway status `FAILED` records failure reason and emits `PaymentFailedEvent` | **PASS** |
| **PAY-09** | Cancellation workflow | `POST /api/v1/bookings/{ref}/cancel` releases locked seats and marks status CANCELLED | **PASS** |
| **PAY-10** | Webhook reaches backend | `POST /api/v1/payments/webhooks/razorpay` and `/stripe` handled by `PaymentWebhookController` | **PASS** |
| **PAY-11** | HMAC signature verification | `HmacUtils.verifySignature()` validates SHA256 signature; rejects counterfeit callbacks (400) | **PASS** |
| **PAY-12** | Duplicate webhook is idempotent | Redis callback deduplication (`idempotencyService.isCallbackAlreadyProcessed`) | **PASS** |
| **PAY-13** | Payment status persistence | Status tracked explicitly: `INITIATED` -> `SUCCESS` / `FAILED` / `REFUNDED` | **PASS** |
| **PAY-14** | Verified booking status transition | Booking transitions to CONFIRMED only upon successful payment confirmation | **PASS** |
| **PAY-15** | Seats confirmed only after valid payment | `BookingServiceImpl:confirmBooking()` calls `showClient.updateShowSeatsStatus(..., "BOOKED")` | **PASS** |
| **PAY-16** | Failed payment releases/cancels seats | Expired/failed bookings release locks automatically via Redis TTL and manual release | **PASS** |
| **PAY-17** | Refreshing payment page safety | If page reloads, pending booking reference is preserved; no duplicate payment created | **PASS** |
| **PAY-18** | Double-click prevention on Pay button | UI flag `state.isProcessingPayment` immediately disables `#btnPay` on first click | **PASS** |
| **PAY-19** | Network timeout idempotency | Caller-supplied `idempotencyKey` cached in Redis + DB prevents double charging on retry | **PASS** |
| **PAY-20** | Clear user error messaging | Payment errors trigger user-friendly toast messages; no raw JSON or uncaught errors | **PASS** |
| **PAY-21** | Zero payment secrets in browser | Gateway API keys and webhook secrets reside exclusively in backend environment config | **PASS** |
| **PAY-22** | Zero live secrets committed to Git | Values files and repository scanned for `rzp_live_` and `sk_live_` (Clean) | **PASS** |

---

## 3. Supported Payment Providers

1. **MOCK (Default for Local/Test/CI)**:
   - `PaymentClientImpl` simulates gateway initiation, returns deterministic `TXN-...` reference.
   - Allows complete end-to-end verification without third-party internet dependencies or charges.
2. **RAZORPAY**:
   - `RazorpayPaymentClient` connects to `https://api.razorpay.com/v1/orders`.
   - Webhook signature validated via HMAC-SHA256 with `x-razorpay-signature`.
3. **STRIPE**:
   - `StripePaymentClient` creates PaymentIntents against `https://api.stripe.com/v1/payment_intents`.
   - Webhook signature validated with `Stripe-Signature`.

---

## 4. Verification Verdict

The payment flow guarantees strong consistency, zero duplicate payments through two-tier idempotency, and cryptographic webhook verification.
