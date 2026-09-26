# Operational Runbook: Payment Failure Spike & Webhook Inconsistencies

**Runbook ID:** RB-OPS-006  
**Target:** `payment-service`, Payment Gateway Integrations (Razorpay / Stripe / Mock)  
**Severity:** P0 (Financial / Revenue Impacting)

---

## 1. Symptoms
- Prometheus alert: `PaymentFailureRateSpike` or `WebhookSignatureVerificationFailure` firing.
- Customers charged on credit card / UPI, but booking remains in `PENDING` or transitions to `EXPIRED`.
- Elevated HTTP 500 or 400 responses on `/api/v1/payments/initiate` or `/api/v1/payments/webhook`.
- Duplicate payment attempts or idempotency key collision errors.

---

## 2. Initial Checks
1. Identify payment provider in use: `MOCK`, `RAZORPAY`, or `STRIPE`.
2. Verify third-party payment provider status page (Razorpay/Stripe API uptime).
3. Check webhook endpoint responsiveness and SSL certificate validity.
4. Verify webhook secret configuration (`PAYMENT_WEBHOOK_SECRET`, `RAZORPAY_WEBHOOK_SECRET`).

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check payment-service container status
docker compose ps payment-service

# Check payment transactions status in MySQL
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT status, count(*) FROM vibecheck_payment.payments GROUP BY status;"

# Check recent failed payment records
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT id, booking_id, amount, status, failure_reason, created_at 
  FROM vibecheck_payment.payments 
  WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 10;"

# Check webhook log / idempotency table
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT event_id, status, created_at FROM vibecheck_payment.payment_webhooks 
  ORDER BY created_at DESC LIMIT 10;"
```

### Kubernetes:
```bash
# Check payment-service pods and logs
kubectl get pods -n vibecheck -l app.kubernetes.io/name=payment-service
kubectl logs -n vibecheck -l app.kubernetes.io/name=payment-service --tail=200
```

---

## 4. Logs to Inspect
```bash
# Docker Compose: grep for payment errors and webhook issues
docker compose logs --tail=300 vibecheck-payment | grep -iE "PaymentException|InvalidSignature|WebhookProcessingException|PaymentGatewayException"

# Check Kafka events for payment completion
docker compose logs --tail=200 vibecheck-booking | grep -iE "payment-completed|payment.completed"
```

---

## 5. Metrics to Inspect
- `vibecheck_payment_initiated_total`
- `vibecheck_payment_success_total`
- `vibecheck_payment_failed_total`
- `vibecheck_webhook_processed_total{status="FAILURE"}`
- `http_server_requests_seconds_count{uri="/api/v1/payments/webhook", status=~"5.."}`

---

## 6. Safe Recovery Procedure

### Scenario A: Webhook Delivery Delay / Missing Webhook
If the payment gateway succeeded externally but the webhook failed to deliver:
1. Identify the unconfirmed payment record in `vibecheck_payment.payments`:
   ```sql
   SELECT id, booking_id, external_payment_id, status FROM vibecheck_payment.payments WHERE booking_id = 'xxx';
   ```
2. Trigger manual verification against the payment gateway API or re-post the webhook payload:
   ```bash
   curl -X POST http://localhost:8079/api/v1/payments/verify \
     -H "Authorization: Bearer <user_token>" \
     -H "Content-Type: application/json" \
     -d '{"paymentId": "<payment_id>", "bookingId": "<booking_id>"}'
   ```
3. Once verified, `payment-service` publishes `payment-completed` event to Kafka, which instructs `booking-service` to transition booking to `CONFIRMED`.

### Scenario B: Webhook Signature Mismatch
If `InvalidSignatureException` is logged:
1. Verify that the configured webhook secret in the environment matches the portal secret:
   ```bash
   # Check active secret in container
   docker compose exec payment-service env | grep -i SECRET
   ```
2. Update Kubernetes secret / Docker Compose `.env` with the correct signing secret.
3. Restart payment service to load updated configuration.

### Scenario C: Payment Succeeded but Booking Expired Race Condition
If payment was confirmed after booking expired (e.g. user took 12 minutes to enter OTP):
1. Identify transaction: status is `SUCCESS` in payment-service, but `EXPIRED` in booking-service.
2. The platform's automated refund handler will issue a refund via the payment provider API.
3. Verify refund status in `vibecheck_payment.refunds`.

---

## 7. Verification
1. Initiate a test payment using the Mock provider:
   ```bash
   curl -X POST http://localhost:8079/api/v1/payments/initiate \
     -H "Authorization: Bearer <test_user_token>" \
     -H "Content-Type: application/json" \
     -d '{"bookingId": "<test_booking_id>", "amount": 250.00, "provider": "MOCK"}'
   ```
2. Verify response contains `paymentId` and status `PROCESSING` or `SUCCESS`.
3. Verify Kafka topic `payment-events` received the notification.

---

## 8. Rollback Procedure
If a recent deployment broke gateway integrations:
- Roll back payment-service deployment:
  `kubectl rollout undo deployment/payment-service -n vibecheck`

---

## 9. Escalation Conditions
- External payment gateway returns 5xx for all requests (gateway outage).
- Discrepancy between bank capture reports and platform database exceeds $500.
- Webhook endpoints fail to respond for > 5 consecutive minutes.
