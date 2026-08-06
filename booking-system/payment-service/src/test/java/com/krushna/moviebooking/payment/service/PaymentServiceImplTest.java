package com.krushna.moviebooking.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krushna.moviebooking.payment.dto.*;
import com.krushna.moviebooking.payment.entity.Payment;
import com.krushna.moviebooking.payment.event.PaymentEventPublisher;
import com.krushna.moviebooking.payment.exception.PaymentNotFoundException;
import com.krushna.moviebooking.payment.gateway.PaymentClient;
import com.krushna.moviebooking.payment.gateway.PaymentGatewayException;
import com.krushna.moviebooking.payment.gateway.PaymentGatewayFactory;
import com.krushna.moviebooking.payment.repository.PaymentRepository;
import com.krushna.moviebooking.payment.service.impl.PaymentServiceImpl;
import com.krushna.moviebooking.payment.validator.PaymentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentClient paymentClient;
    @Mock private PaymentGatewayFactory paymentGatewayFactory;
    @Mock private PaymentIdempotencyService idempotencyService;
    @Mock private PaymentValidator paymentValidator;
    @Mock private PaymentEventPublisher eventPublisher;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID paymentId;
    private UUID bookingId;
    private UUID userId;
    private String idempotencyKey;
    private String txnRef;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        userId = UUID.randomUUID();
        idempotencyKey = "idem-key-" + UUID.randomUUID();
        txnRef = "MOCK_GATEWAY-TXN12345-" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("initiatePayment: persists INITIATED record and returns response on first call")
    void initiatePayment_FirstCall_PersistsAndReturns() {
        PaymentRequest request = buildRequest();
        Payment savedPayment = buildPayment("INITIATED");
        PaymentResponse gwResponse = buildGatewayResponse(savedPayment);

        when(idempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentGatewayFactory.getPaymentClient()).thenReturn(paymentClient);
        when(paymentClient.getGatewayName()).thenReturn("MOCK");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentClient.initiatePayment(request)).thenReturn(gwResponse);

        PaymentResponse result = paymentService.initiatePayment(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("INITIATED");
        verify(paymentValidator).validatePaymentRequest(request);
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(idempotencyService).cacheResponse(eq(idempotencyKey), any(PaymentResponse.class));
    }

    @Test
    @DisplayName("initiatePayment: returns cached response on duplicate idempotency key without calling gateway")
    void initiatePayment_DuplicateKey_ReturnsCachedWithoutGatewayCall() {
        PaymentRequest request = buildRequest();
        PaymentResponse cached = buildGatewayResponse(buildPayment("INITIATED"));

        when(idempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.of(cached));

        PaymentResponse result = paymentService.initiatePayment(request);

        assertThat(result.status()).isEqualTo("INITIATED");
        verify(paymentClient, never()).initiatePayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiatePayment: marks payment FAILED and rethrows when gateway throws PaymentGatewayException")
    void initiatePayment_GatewayFailure_MarksFailedAndRethrows() {
        PaymentRequest request = buildRequest();
        Payment savedPayment = buildPayment("INITIATED");

        when(idempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentGatewayFactory.getPaymentClient()).thenReturn(paymentClient);
        when(paymentClient.getGatewayName()).thenReturn("MOCK");
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentClient.initiatePayment(request))
                .thenThrow(new PaymentGatewayException("Gateway rejected request"));

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("Gateway rejected request");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        Payment failedSave = captor.getAllValues().stream()
                .filter(p -> "FAILED".equals(p.getStatus()))
                .findFirst()
                .orElse(null);
        assertThat(failedSave).isNotNull();
    }

    @Test
    @DisplayName("processCallback: updates payment to SUCCESS")
    void processCallback_Success_UpdatesStatus() {
        PaymentCallback callback = buildCallback("SUCCESS");
        Payment payment = buildPayment("INITIATED");

        when(idempotencyService.isCallbackAlreadyProcessed(txnRef)).thenReturn(false);
        when(paymentRepository.findByTransactionReference(txnRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse result = paymentService.processCallback(callback);

        assertThat(result).isNotNull();
        verify(paymentValidator).validateCallback(callback);
        verify(paymentRepository).save(any(Payment.class));
        verify(idempotencyService).markCallbackProcessed(txnRef);
    }

    @Test
    @DisplayName("processRefund: processes refund and updates entity status")
    void processRefund_Success_UpdatesEntity() {
        RefundRequest request = RefundRequest.builder()
                .paymentId(paymentId)
                .amount(new BigDecimal("300.00"))
                .reason("User request")
                .idempotencyKey("refund-idem-1")
                .build();

        Payment payment = buildPayment("SUCCESS");

        RefundResponse refundResponse = RefundResponse.builder()
                .refundId(UUID.randomUUID())
                .paymentId(paymentId)
                .refundReference("RFD-12345")
                .amount(new BigDecimal("300.00"))
                .currency("INR")
                .status("REFUNDED")
                .reason("User request")
                .createdAt(Instant.now())
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentGatewayFactory.getPaymentClient(payment.getPaymentGateway())).thenReturn(paymentClient);
        when(paymentClient.processRefund(request)).thenReturn(refundResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        RefundResponse result = paymentService.processRefund(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("REFUNDED");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("getPaymentById: returns payment response when found")
    void getPaymentById_Found_ReturnsResponse() {
        Payment payment = buildPayment("SUCCESS");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse result = paymentService.getPaymentById(paymentId);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getPaymentById: throws PaymentNotFoundException when not found")
    void getPaymentById_NotFound_ThrowsException() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    private PaymentRequest buildRequest() {
        return PaymentRequest.builder()
                .bookingId(bookingId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .paymentMethod("UPI")
                .bookingReference("BK1234567890")
                .build();
    }

    private Payment buildPayment(String status) {
        Payment p = new Payment();
        p.setId(paymentId);
        p.setBookingId(bookingId);
        p.setUserId(userId);
        p.setIdempotencyKey(idempotencyKey);
        p.setPaymentGateway("MOCK");
        p.setTransactionReference(txnRef);
        p.setAmount(new BigDecimal("500.00"));
        p.setCurrency("INR");
        p.setPaymentMethod("UPI");
        p.setStatus(status);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private PaymentResponse buildGatewayResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .idempotencyKey(payment.getIdempotencyKey())
                .transactionReference(txnRef)
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .redirectUrl("https://mock-gateway.test/checkout/BK1234567890")
                .createdAt(Instant.now())
                .build();
    }

    private PaymentCallback buildCallback(String status) {
        return PaymentCallback.builder()
                .transactionReference(txnRef)
                .gatewayStatus(status)
                .gatewayName("MOCK")
                .failureReason(status.equals("FAILED") ? "Insufficient funds" : null)
                .signature("dummy-signature")
                .build();
    }
}
