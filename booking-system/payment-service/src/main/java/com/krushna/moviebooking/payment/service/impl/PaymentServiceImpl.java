package com.krushna.moviebooking.payment.service.impl;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.entity.Payment;
import com.krushna.moviebooking.payment.event.PaymentEventPublisher;
import com.krushna.moviebooking.payment.event.PaymentFailedEvent;
import com.krushna.moviebooking.payment.event.PaymentInitiatedEvent;
import com.krushna.moviebooking.payment.event.PaymentSuccessEvent;
import com.krushna.moviebooking.payment.exception.PaymentNotFoundException;
import com.krushna.moviebooking.payment.gateway.PaymentClient;
import com.krushna.moviebooking.payment.gateway.PaymentGatewayException;
import com.krushna.moviebooking.payment.repository.PaymentRepository;
import com.krushna.moviebooking.payment.service.PaymentIdempotencyService;
import com.krushna.moviebooking.payment.service.PaymentService;
import com.krushna.moviebooking.payment.validator.PaymentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary implementation of {@link PaymentService}.
 *
 * <p><b>Idempotency strategy</b>:
 * <ol>
 *   <li>Check Redis for a cached {@link PaymentResponse} matching the {@code idempotencyKey}.</li>
 *   <li>If found → return cached result immediately (no gateway call, no DB write).</li>
 *   <li>If not found → persist INITIATED record → call gateway → update record → cache result.</li>
 * </ol>
 *
 * <p><b>Callback deduplication</b>:
 * <ol>
 *   <li>Check Redis for a processed marker on the {@code transactionReference}.</li>
 *   <li>If found → return existing DB record idempotently.</li>
 *   <li>If not found → update Payment status → mark Redis → publish event.</li>
 * </ol>
 *
 * <p><b>Transactional boundaries</b>:
 * <ul>
 *   <li>All write methods are {@code @Transactional(REQUIRED)} — DB is committed before
 *       Kafka events are published to prevent event-before-commit races.</li>
 *   <li>Read methods are {@code @Transactional(readOnly = true)}.</li>
 * </ul>
 *
 * <p><b>Retry</b>: Gateway calls are retried automatically via Spring Retry annotations on
 * {@link PaymentClient} for {@link com.krushna.moviebooking.payment.gateway.PaymentTimeoutException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository         paymentRepository;
    private final PaymentClient             paymentClient;
    private final PaymentIdempotencyService idempotencyService;
    private final PaymentValidator          paymentValidator;
    private final PaymentEventPublisher     eventPublisher;

    // -------------------------------------------------------------------------
    // INITIATE
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Database write is transactional. Gateway call is outside the DB transaction
     * to avoid holding a connection during a potentially slow HTTP call.
     */
    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("[PaymentService] Initiating payment | idempotencyKey={} bookingRef={} amount={}",
                request.idempotencyKey(), request.bookingReference(), request.amount());

        // Step 1 — Validate business rules
        paymentValidator.validatePaymentRequest(request);

        // Step 2 — Idempotency check: return cached response if already processed
        Optional<PaymentResponse> cached = idempotencyService.findCachedResponse(request.idempotencyKey());
        if (cached.isPresent()) {
            log.info("[PaymentService] Idempotent hit for key={} — returning cached response", request.idempotencyKey());
            return cached.get();
        }

        // Step 3 — Persist INITIATED payment record
        Payment payment = Payment.builder()
                .bookingId(request.bookingId())
                .userId(request.userId())
                .idempotencyKey(request.idempotencyKey())
                .paymentGateway("MOCK_GATEWAY")
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "INR")
                .paymentMethod(request.paymentMethod())
                .status("INITIATED")
                .build();

        payment = paymentRepository.save(payment);
        final UUID paymentId = payment.getId();
        log.info("[PaymentService] Payment record persisted | paymentId={} idempotencyKey={}", paymentId, request.idempotencyKey());

        // Step 4 — Call external gateway (Spring Retry handles timeouts transparently)
        PaymentResponse gatewayResponse;
        try {
            gatewayResponse = paymentClient.initiatePayment(request);
        } catch (PaymentGatewayException ex) {
            log.error("[PaymentService] Gateway failure for paymentId={}: {}", paymentId, ex.getMessage());
            payment.setStatus("FAILED");
            payment.setFailureReason(ex.getMessage());
            paymentRepository.save(payment);
            publishFailedEvent(payment, ex.getMessage());
            throw ex;
        }

        // Step 5 — Update record with gateway-assigned transaction reference
        payment.setTransactionReference(gatewayResponse.transactionReference());
        payment = paymentRepository.save(payment);

        // Step 6 — Build response and cache it
        PaymentResponse response = buildResponse(payment, gatewayResponse.redirectUrl());
        idempotencyService.cacheResponse(request.idempotencyKey(), response);

        // Step 7 — Publish domain event
        publishInitiatedEvent(payment, request);

        log.info("[PaymentService] Payment initiated successfully | paymentId={} txnRef={}",
                paymentId, payment.getTransactionReference());
        return response;
    }

    // -------------------------------------------------------------------------
    // CALLBACK
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Duplicate callbacks for the same {@code transactionReference} are detected via Redis
     * and handled idempotently without re-publishing events.
     */
    @Override
    @Transactional
    public PaymentResponse processCallback(PaymentCallback callback) {
        log.info("[PaymentService] Processing callback | txnRef={} status={} gateway={}",
                callback.transactionReference(), callback.gatewayStatus(), callback.gatewayName());

        // Step 1 — Validate signature and payload
        paymentValidator.validateCallback(callback);

        // Step 2 — Duplicate callback guard
        if (idempotencyService.isCallbackAlreadyProcessed(callback.transactionReference())) {
            log.info("[PaymentService] Duplicate callback for txnRef={} — returning existing record",
                    callback.transactionReference());
            Payment existing = paymentRepository
                    .findByTransactionReference(callback.transactionReference())
                    .orElseThrow(() -> new PaymentNotFoundException(
                            "Payment not found for txnRef: " + callback.transactionReference()));
            return buildResponse(existing, null);
        }

        // Step 3 — Locate payment record
        Payment payment = paymentRepository
                .findByTransactionReference(callback.transactionReference())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for txnRef: " + callback.transactionReference()));

        // Step 4 — Reconcile status
        String mappedStatus = mapGatewayStatus(callback.gatewayStatus());
        payment.setStatus(mappedStatus);
        if (callback.failureReason() != null) {
            payment.setFailureReason(callback.failureReason());
        }
        payment = paymentRepository.save(payment);
        log.info("[PaymentService] Payment status updated | paymentId={} status={}", payment.getId(), mappedStatus);

        // Step 5 — Mark callback as processed (deduplication)
        idempotencyService.markCallbackProcessed(callback.transactionReference());

        // Step 6 — Publish domain event
        publishCallbackEvent(payment, mappedStatus);

        return buildResponse(payment, null);
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        log.debug("[PaymentService] Fetching payment by id={}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));
        return buildResponse(payment, null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(UUID bookingId) {
        log.debug("[PaymentService] Fetching payment for bookingId={}", bookingId);
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for bookingId: " + bookingId));
        return buildResponse(payment, null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUserId(UUID userId, Pageable pageable) {
        log.debug("[PaymentService] Fetching payments for userId={}", userId);
        return paymentRepository.findByUserId(userId, pageable)
                .map(p -> buildResponse(p, null));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private PaymentResponse buildResponse(Payment payment, String redirectUrl) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .idempotencyKey(payment.getIdempotencyKey())
                .transactionReference(payment.getTransactionReference())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .redirectUrl(redirectUrl)
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private String mapGatewayStatus(String gatewayStatus) {
        return switch (gatewayStatus.toUpperCase()) {
            case "SUCCESS" -> "SUCCESS";
            case "FAILED"  -> "FAILED";
            default        -> "INITIATED";
        };
    }

    private void publishInitiatedEvent(Payment payment, PaymentRequest request) {
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .idempotencyKey(payment.getIdempotencyKey())
                .transactionReference(payment.getTransactionReference())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishPaymentInitiated(event);
    }

    private void publishCallbackEvent(Payment payment, String status) {
        if ("SUCCESS".equals(status)) {
            PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                    .paymentId(payment.getId())
                    .bookingId(payment.getBookingId())
                    .userId(payment.getUserId())
                    .transactionReference(payment.getTransactionReference())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .timestamp(Instant.now())
                    .build();
            eventPublisher.publishPaymentSuccess(event);
        } else if ("FAILED".equals(status)) {
            publishFailedEvent(payment, payment.getFailureReason());
        }
    }

    private void publishFailedEvent(Payment payment, String reason) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .transactionReference(payment.getTransactionReference())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .failureReason(reason)
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishPaymentFailed(event);
    }
}
