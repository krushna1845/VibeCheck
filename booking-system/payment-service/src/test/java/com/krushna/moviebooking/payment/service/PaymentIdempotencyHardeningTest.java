package com.krushna.moviebooking.payment.service;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.dto.RefundRequest;
import com.krushna.moviebooking.payment.dto.RefundResponse;
import com.krushna.moviebooking.payment.entity.Payment;
import com.krushna.moviebooking.payment.gateway.PaymentClient;
import com.krushna.moviebooking.payment.gateway.PaymentGatewayFactory;
import com.krushna.moviebooking.payment.repository.PaymentRepository;
import com.krushna.moviebooking.payment.service.impl.PaymentServiceImpl;
import com.krushna.moviebooking.payment.validator.PaymentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.payment.event.PaymentEventPublisher;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Idempotency Hardening & Deduplication Tests")
class PaymentIdempotencyHardeningTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGatewayFactory paymentGatewayFactory;
    @Mock private PaymentClient paymentClient;
    @Mock private PaymentIdempotencyService idempotencyService;
    @Mock private PaymentValidator paymentValidator;
    @Mock private PaymentEventPublisher eventPublisher;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID bookingId;
    private UUID userId;
    private UUID paymentId;
    private String idempotencyKey;
    private PaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {

        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        idempotencyKey = "idem-" + UUID.randomUUID();

        paymentRequest = PaymentRequest.builder()
                .bookingId(bookingId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .amount(new BigDecimal("750.00"))
                .currency("INR")
                .paymentMethod("CARD")
                .bookingReference("BK1234567890")
                .build();
    }

    @Test
    @DisplayName("TEST: Duplicate payment with same idempotency key returns same payment without charging again")
    void duplicatePayment_ReturnsSamePayment_NoMultipleCharges() {
        PaymentResponse cached = PaymentResponse.builder()
                .paymentId(paymentId)
                .bookingId(bookingId)
                .idempotencyKey(idempotencyKey)
                .amount(new BigDecimal("750.00"))
                .currency("INR")
                .status("INITIATED")
                .transactionReference("TXN-CACHED-001")
                .build();

        when(idempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.of(cached));

        PaymentResponse result = paymentService.initiatePayment(paymentRequest);

        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.transactionReference()).isEqualTo("TXN-CACHED-001");
        verify(paymentGatewayFactory, never()).getPaymentClient();
        verify(paymentClient, never()).initiatePayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("TEST: Network response lost after successful payment initiation -> subsequent idempotent request does not charge again (DB fallback)")
    void networkResponseLost_SubsequentIdempotentRequest_DoesNotChargeAgain() {
        // Cache miss in Redis (e.g. key evicted or lost network response)
        when(idempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.empty());

        // But DB already contains the payment record with this idempotency key
        Payment dbPayment = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .amount(new BigDecimal("750.00"))
                .currency("INR")
                .paymentGateway("MOCK")
                .status("INITIATED")
                .transactionReference("TXN-DB-PREVIOUS")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(dbPayment));

        PaymentResponse result = paymentService.initiatePayment(paymentRequest);

        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.transactionReference()).isEqualTo("TXN-DB-PREVIOUS");
        // Must NOT initiate with gateway client again!
        verify(paymentGatewayFactory, never()).getPaymentClient();
        verify(paymentClient, never()).initiatePayment(any());
        // Must re-cache in Redis for faster subsequent lookups
        verify(idempotencyService).cacheResponse(eq(idempotencyKey), any(PaymentResponse.class));
    }

    @Test
    @DisplayName("TEST: Duplicate refund with same idempotency key or already refunded payment -> no duplicate refund")
    void duplicateRefund_ReturnsSameRefund_NoDuplicateGatewayCall() {
        String refundIdemKey = "refund-idem-999";
        RefundRequest refundRequest = RefundRequest.builder()
                .paymentId(paymentId)
                .amount(new BigDecimal("750.00"))
                .reason("User cancelled")
                .idempotencyKey(refundIdemKey)
                .build();

        // 1. First test: Redis refund cache hit
        RefundResponse cachedRefund = RefundResponse.builder()
                .paymentId(paymentId)
                .bookingId(bookingId)
                .refundReference("RFD-12345")
                .amount(new BigDecimal("750.00"))
                .status("REFUNDED")
                .build();
        when(idempotencyService.findCachedRefundResponse(refundIdemKey)).thenReturn(Optional.of(cachedRefund));

        RefundResponse resp1 = paymentService.processRefund(refundRequest);
        assertThat(resp1.refundReference()).isEqualTo("RFD-12345");
        verify(paymentGatewayFactory, never()).getPaymentClient();

        // 2. Second test: Redis refund cache miss, but payment is already marked REFUNDED in DB
        when(idempotencyService.findCachedRefundResponse(refundIdemKey)).thenReturn(Optional.empty());
        Payment refundedPayment = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .status("SUCCESS")
                .refundStatus("REFUNDED")
                .refundReference("RFD-FROM-DB")
                .refundAmount(new BigDecimal("750.00"))
                .updatedAt(Instant.now())
                .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(refundedPayment));

        RefundResponse resp2 = paymentService.processRefund(refundRequest);
        assertThat(resp2.refundReference()).isEqualTo("RFD-FROM-DB");
        assertThat(resp2.status()).isEqualTo("REFUNDED");
        verify(paymentGatewayFactory, never()).getPaymentClient();
        verify(idempotencyService).cacheRefundResponse(eq(refundIdemKey), any(RefundResponse.class));
    }

    @Test
    @DisplayName("TEST: Duplicate callback with same transaction reference -> idempotent return")
    void duplicateCallback_ReturnsExistingPayment() {
        String txnRef = "TXN-CALLBACK-123";
        PaymentCallback callback = PaymentCallback.builder()
                .transactionReference(txnRef)
                .gatewayStatus("SUCCESS")
                .gatewayName("MOCK")
                .build();

        when(idempotencyService.isCallbackAlreadyProcessed(txnRef)).thenReturn(true);
        Payment existingPayment = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .status("SUCCESS")
                .transactionReference(txnRef)
                .build();
        when(paymentRepository.findByTransactionReference(txnRef)).thenReturn(Optional.of(existingPayment));

        PaymentResponse response = paymentService.processCallback(callback);

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.transactionReference()).isEqualTo(txnRef);
        // Must NOT save or mark processed again
        verify(paymentRepository, never()).save(any());
        verify(idempotencyService, never()).markCallbackProcessed(any());
    }
}
