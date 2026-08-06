package com.krushna.moviebooking.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.payment.dto.*;
import com.krushna.moviebooking.payment.entity.Payment;
import com.krushna.moviebooking.payment.event.*;
import com.krushna.moviebooking.payment.exception.PaymentNotFoundException;
import com.krushna.moviebooking.payment.gateway.PaymentClient;
import com.krushna.moviebooking.payment.gateway.PaymentGatewayException;
import com.krushna.moviebooking.payment.gateway.PaymentGatewayFactory;
import com.krushna.moviebooking.payment.gateway.RazorpayPaymentClient;
import com.krushna.moviebooking.payment.gateway.StripePaymentClient;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced production implementation of {@link PaymentService} supporting multi-gateway routing (Razorpay, Stripe, Mock),
 * HMAC webhook signature validation, refund processing, and post-commit transactional Kafka event publishing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final PaymentIdempotencyService idempotencyService;
    private final PaymentValidator paymentValidator;
    private final PaymentEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // INITIATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("[PaymentService] Initiating payment | idempotencyKey={} bookingRef={} amount={}",
                request.idempotencyKey(), request.bookingReference(), request.amount());

        paymentValidator.validatePaymentRequest(request);

        Optional<PaymentResponse> cached = idempotencyService.findCachedResponse(request.idempotencyKey());
        if (cached.isPresent()) {
            log.info("[PaymentService] Idempotent hit for key={} — returning cached response", request.idempotencyKey());
            return cached.get();
        }

        PaymentClient client = paymentGatewayFactory.getPaymentClient();
        String gatewayName = client.getGatewayName();

        Payment payment = Payment.builder()
                .bookingId(request.bookingId())
                .userId(request.userId())
                .idempotencyKey(request.idempotencyKey())
                .paymentGateway(gatewayName)
                .amount(request.amount())
                .currency(request.currency() != null ? request.currency() : "INR")
                .paymentMethod(request.paymentMethod())
                .status("INITIATED")
                .build();

        payment = paymentRepository.save(payment);
        final UUID paymentId = payment.getId();
        log.info("[PaymentService] Payment record persisted | paymentId={} gateway={}", paymentId, gatewayName);

        PaymentResponse gatewayResponse;
        try {
            gatewayResponse = client.initiatePayment(request);
        } catch (PaymentGatewayException ex) {
            log.error("[PaymentService] Gateway failure for paymentId={}: {}", paymentId, ex.getMessage());
            payment.setStatus("FAILED");
            payment.setFailureReason(ex.getMessage());
            Payment failedPayment = paymentRepository.save(payment);
            executeAfterCommit(() -> publishFailedEvent(failedPayment, ex.getMessage()));
            throw ex;
        }

        payment.setTransactionReference(gatewayResponse.transactionReference());
        payment = paymentRepository.save(payment);

        PaymentResponse response = buildResponse(payment, gatewayResponse.redirectUrl());
        idempotencyService.cacheResponse(request.idempotencyKey(), response);

        final Payment finalPayment = payment;
        executeAfterCommit(() -> publishInitiatedEvent(finalPayment, request));

        log.info("[PaymentService] Payment initiated successfully | paymentId={} txnRef={}",
                paymentId, payment.getTransactionReference());
        return response;
    }

    // -------------------------------------------------------------------------
    // CALLBACK & WEBHOOKS
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse processCallback(PaymentCallback callback) {
        log.info("[PaymentService] Processing callback | txnRef={} status={} gateway={}",
                callback.transactionReference(), callback.gatewayStatus(), callback.gatewayName());

        paymentValidator.validateCallback(callback);

        if (idempotencyService.isCallbackAlreadyProcessed(callback.transactionReference())) {
            log.info("[PaymentService] Duplicate callback for txnRef={} — returning existing record",
                    callback.transactionReference());
            Payment existing = paymentRepository
                    .findByTransactionReference(callback.transactionReference())
                    .orElseThrow(() -> new PaymentNotFoundException(
                            "Payment not found for txnRef: " + callback.transactionReference()));
            return buildResponse(existing, null);
        }

        Payment payment = paymentRepository
                .findByTransactionReference(callback.transactionReference())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for txnRef: " + callback.transactionReference()));

        String mappedStatus = mapGatewayStatus(callback.gatewayStatus());
        payment.setStatus(mappedStatus);
        if (callback.failureReason() != null) {
            payment.setFailureReason(callback.failureReason());
        }
        payment = paymentRepository.save(payment);
        log.info("[PaymentService] Payment status updated | paymentId={} status={}", payment.getId(), mappedStatus);

        idempotencyService.markCallbackProcessed(callback.transactionReference());

        final Payment savedPayment = payment;
        executeAfterCommit(() -> publishCallbackEvent(savedPayment, mappedStatus));

        return buildResponse(payment, null);
    }

    @Override
    @Transactional
    public PaymentResponse processWebhook(String provider, String rawPayload, Map<String, String> headers) {
        log.info("[PaymentService] Processing webhook | provider={}", provider);

        PaymentClient client = paymentGatewayFactory.getPaymentClient(provider);

        String txnRef = null;
        String status = "SUCCESS";
        String failureReason = null;

        if ("RAZORPAY".equalsIgnoreCase(provider) && client instanceof RazorpayPaymentClient razorpayClient) {
            try {
                JsonNode root = objectMapper.readTree(rawPayload);
                JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
                String orderId = paymentEntity.path("order_id").asText();
                String paymentId = paymentEntity.path("id").asText();
                txnRef = orderId.isEmpty() ? paymentId : orderId;

                String signature = headers.get("x-razorpay-signature");
                if (signature == null) {
                    signature = headers.get("X-Razorpay-Signature");
                }

                if (!razorpayClient.verifyWebhookSignature(orderId, paymentId, signature)) {
                    log.error("[PaymentService] Invalid Razorpay webhook signature");
                    throw new PaymentGatewayException("Invalid Razorpay webhook signature");
                }

                String eventType = root.path("event").asText();
                if ("payment.failed".equalsIgnoreCase(eventType)) {
                    status = "FAILED";
                    failureReason = paymentEntity.path("error_description").asText("Razorpay payment failed");
                }
            } catch (Exception e) {
                if (e instanceof PaymentGatewayException pge) throw pge;
                throw new PaymentGatewayException("Failed to parse Razorpay webhook payload", e);
            }
        } else if ("STRIPE".equalsIgnoreCase(provider) && client instanceof StripePaymentClient stripeClient) {
            try {
                String signatureHeader = headers.get("stripe-signature");
                if (signatureHeader == null) {
                    signatureHeader = headers.get("Stripe-Signature");
                }

                if (!stripeClient.verifyWebhookSignature(rawPayload, signatureHeader)) {
                    log.error("[PaymentService] Invalid Stripe webhook signature");
                    throw new PaymentGatewayException("Invalid Stripe webhook signature");
                }

                JsonNode root = objectMapper.readTree(rawPayload);
                JsonNode dataObj = root.path("data").path("object");
                txnRef = dataObj.path("id").asText();
                String eventType = root.path("type").asText();

                if (eventType.contains("failed")) {
                    status = "FAILED";
                    failureReason = dataObj.path("last_payment_error").path("message").asText("Stripe payment failed");
                }
            } catch (Exception e) {
                if (e instanceof PaymentGatewayException pge) throw pge;
                throw new PaymentGatewayException("Failed to parse Stripe webhook payload", e);
            }
        } else {
            // Default mock callback mapping
            PaymentCallback callback = PaymentCallback.builder()
                    .transactionReference(headers.getOrDefault("txn-ref", "TXN-" + UUID.randomUUID()))
                    .gatewayStatus(headers.getOrDefault("status", "SUCCESS"))
                    .gatewayName(provider)
                    .build();
            return processCallback(callback);
        }

        PaymentCallback callback = PaymentCallback.builder()
                .transactionReference(txnRef)
                .gatewayStatus(status)
                .gatewayName(provider)
                .failureReason(failureReason)
                .build();

        return processCallback(callback);
    }

    // -------------------------------------------------------------------------
    // REFUND
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public RefundResponse processRefund(RefundRequest request) {
        log.info("[PaymentService] Processing refund | paymentId={} amount={}", request.paymentId(), request.amount());

        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for id: " + request.paymentId()));

        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new IllegalStateException("Cannot refund payment with status: " + payment.getStatus());
        }

        PaymentClient client = paymentGatewayFactory.getPaymentClient(payment.getPaymentGateway());
        RefundResponse refundResponse = client.processRefund(request);

        payment.setRefundReference(refundResponse.refundReference());
        payment.setRefundAmount(request.amount());
        payment.setRefundStatus("REFUNDED");
        payment = paymentRepository.save(payment);

        final Payment savedPayment = payment;
        final RefundResponse finalResponse = refundResponse;
        executeAfterCommit(() -> publishRefundEvent(savedPayment, finalResponse));

        log.info("[PaymentService] Refund processed successfully | refundRef={}", refundResponse.refundReference());
        return refundResponse;
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));
        return buildResponse(payment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(UUID bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for bookingId: " + bookingId));
        return buildResponse(payment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByUserId(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(p -> buildResponse(p, null));
    }

    // -------------------------------------------------------------------------
    // Helpers & Event Publishing
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
            case "FAILED" -> "FAILED";
            default -> "INITIATED";
        };
    }

    private void executeAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
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

    private void publishRefundEvent(Payment payment, RefundResponse response) {
        PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                .refundId(response.refundId())
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .refundReference(response.refundReference())
                .transactionReference(payment.getTransactionReference())
                .amount(response.amount())
                .currency(payment.getCurrency())
                .reason(response.reason())
                .timestamp(Instant.now())
                .build();
        eventPublisher.publishPaymentRefunded(event);
    }
}
