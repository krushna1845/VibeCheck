package com.krushna.moviebooking.payment.gateway;

import com.krushna.moviebooking.payment.config.PaymentGatewayProperties;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * Mock implementation of {@link PaymentClient} that simulates an external payment gateway.
 *
 * <p><b>Retry behaviour</b>: The {@code initiatePayment} method is annotated with
 * {@link Retryable} and will automatically retry up to {@code maxAttempts} times with
 * exponential backoff on {@link PaymentTimeoutException}. Non-retryable
 * {@link PaymentGatewayException} propagates immediately.
 *
 * <p><b>Simulation rules</b>:
 * <ul>
 *   <li>10 % of requests simulate a timeout (retryable).</li>
 *   <li>5 % of requests simulate a gateway failure (non-retryable).</li>
 *   <li>Remaining requests succeed and return a checkout URL + txn reference.</li>
 * </ul>
 *
 * <p>Replace this class with a real HTTP/REST adapter in production.
 */
@Slf4j
@Component("MOCK")
@RequiredArgsConstructor
@EnableConfigurationProperties(PaymentGatewayProperties.class)
public class PaymentClientImpl implements PaymentClient {

    private static final String GATEWAY_NAME = "MOCK_GATEWAY";
    private final PaymentGatewayProperties gatewayProperties;
    private final Random random = new Random();

    @Override
    public String getGatewayName() {
        return "MOCK";
    }

    /**
     * Initiates a payment against the mock gateway.
     *
     * <p>Retried automatically on {@link PaymentTimeoutException} with exponential backoff.
     * Propagates {@link PaymentGatewayException} immediately (non-retryable).
     *
     * @param request Validated payment initiation payload
     * @return {@link PaymentResponse} with INITIATED status and a mock checkout URL
     */
    @Override
    @Retryable(
            retryFor = PaymentTimeoutException.class,
            maxAttemptsExpression = "${payment.gateway.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${payment.gateway.retry.initial-delay-ms:500}",
                    multiplierExpression = "${payment.gateway.retry.multiplier:2.0}",
                    maxDelayExpression = "${payment.gateway.retry.max-delay-ms:5000}"
            )
    )
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("[PaymentGateway] Initiating payment | bookingRef={} idempotencyKey={} amount={} {}",
                request.bookingReference(), request.idempotencyKey(),
                request.amount(), request.currency() != null ? request.currency() : "INR");

        simulateGatewayBehaviour(request.idempotencyKey());

        String txnRef = buildTransactionReference(request.idempotencyKey());
        String redirectUrl = gatewayProperties.getBaseUrl() + "/checkout/" + request.bookingReference();

        log.info("[PaymentGateway] Payment initiated successfully | txnRef={} bookingRef={}",
                txnRef, request.bookingReference());

        return PaymentResponse.builder()
                .paymentId(UUID.randomUUID())
                .bookingId(request.bookingId())
                .bookingReference(request.bookingReference())
                .idempotencyKey(request.idempotencyKey())
                .transactionReference(txnRef)
                .status("INITIATED")
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "INR")
                .paymentMethod(request.paymentMethod())
                .redirectUrl(redirectUrl)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Recovery method called when all retry attempts for {@link PaymentTimeoutException} are exhausted.
     * Re-throws the timeout as a {@link PaymentGatewayException} so the service layer can handle it.
     *
     * @param ex      The last timeout exception encountered
     * @param request The original payment request
     * @return never returns normally
     */
    @Recover
    public PaymentResponse recoverFromTimeout(PaymentTimeoutException ex, PaymentRequest request) {
        log.error("[PaymentGateway] All retry attempts exhausted for bookingRef={} idempotencyKey={}. Cause: {}",
                request.bookingReference(), request.idempotencyKey(), ex.getMessage());
        throw new PaymentGatewayException(
                "Gateway timed out after all retry attempts for key: " + request.idempotencyKey(), ex);
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private void simulateGatewayBehaviour(String idempotencyKey) {
        int roll = random.nextInt(100);
        if (roll < 10) {
            log.warn("[PaymentGateway] Simulating TIMEOUT for key={}", idempotencyKey);
            throw new PaymentTimeoutException("Mock gateway timeout for key: " + idempotencyKey);
        }
        if (roll < 15) {
            log.warn("[PaymentGateway] Simulating GATEWAY_FAILURE for key={}", idempotencyKey);
            throw new PaymentGatewayException("Mock gateway failure for key: " + idempotencyKey);
        }
        // Simulate network latency
        try {
            Thread.sleep(random.nextInt(100));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildTransactionReference(String idempotencyKey) {
        // Deterministic prefix from idempotency key for easy tracing
        String suffix = idempotencyKey.length() >= 8
                ? idempotencyKey.substring(idempotencyKey.length() - 8).toUpperCase()
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return GATEWAY_NAME + "-" + suffix + "-" + Instant.now().toEpochMilli();
    }
}
