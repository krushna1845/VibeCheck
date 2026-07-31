package com.krushna.moviebooking.payment.service;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentRequest;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.entity.Payment;
import com.krushna.moviebooking.payment.event.PaymentEventPublisher;
import com.krushna.moviebooking.payment.exception.PaymentNotFoundException;
import com.krushna.moviebooking.payment.gateway.PaymentClient;
import com.krushna.moviebooking.payment.gateway.PaymentGatewayException;
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

/**
 * Unit tests for {@link PaymentServiceImpl} using Mockito.
 *
 * <p>All external dependencies (repository, gateway, idempotency service, validator,
 * event publisher) are mocked to ensure pure, isolated unit-test behaviour.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock private PaymentRepository         paymentRepository;
    @Mock private PaymentClient             paymentClient;
    @Mock private PaymentIdempotencyService idempotencyService;
    @Mock private PaymentValidator          paymentValidator;
    @Mock private PaymentEventPublisher     eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID      paymentId;
    private UUID      bookingId;
    private UUID      userId;
    private String    idempotencyKey;
    private String    txnRef;

    @BeforeEach
    void setUp() {
        paymentId      = UUID.randomUUID();
        bookingId      = UUID.randomUUID();
        userId         = UUID.randomUUID();
        idempotencyKey = "idem-key-" + UUID.randomUUID();
        txnRef         = "MOCK_GATEWAY-TXN12345-" + System.currentTimeMillis();
    }

    // -------------------------------------------------------------------------
    // initiatePayment — happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("initiatePayment: persists INITIATED record and returns response on first call")
    void initiatePayment_FirstCall_PersistsAndReturns() {
        PaymentRequest request = buildRequest();
        Payment savedPayment  = buildPayment("INITIATED");
        PaymentResponse gwResponse = buildGatewayResponse(savedPayment);

        when(idempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentClient.initiatePayment(request)).thenReturn(gwResponse);

        PaymentResponse result = paymentService.initiatePayment(request);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("INITIATED");
        verify(paymentValidator).validatePaymentRequest(request);
        verify(paymentRepository, times(2)).save(any(Payment.class)); // once INITIATED, once with txnRef
        verify(idempotencyService).cacheResponse(eq(idempotencyKey), any(PaymentResponse.class));
        verify(eventPublisher).publishPaymentInitiated(any());
    }

    @Test
    @DisplayName("initiatePayment: returns cached response on duplicate idempotency key without calling gateway")
    void initiatePayment_DuplicateKey_ReturnsCachedWithoutGatewayCall() {
        PaymentRequest request  = buildRequest();
        PaymentResponse cached  = buildGatewayResponse(buildPayment("INITIATED"));

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
        Payment savedPayment   = buildPayment("INITIATED");

        when(idempotencyService.findCachedResponse(idempotencyKey)).thenReturn(Optional.empty());
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
        // First save is INITIATED record; second save sets FAILED
        assertThat(failedSave).isNotNull();
        verify(eventPublisher).publishPaymentFailed(any());
    }

    @Test
    @DisplayName("initiatePayment: validation failure propagates before any DB or gateway call")
    void initiatePayment_ValidationFailure_PropagatesEarly() {
        PaymentRequest request = buildRequest();
        doThrow(new com.krushna.moviebooking.payment.exception.InvalidPaymentRequestException("Bad amount"))
                .when(paymentValidator).validatePaymentRequest(request);

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
                .isInstanceOf(com.krushna.moviebooking.payment.exception.InvalidPaymentRequestException.class);

        verify(paymentRepository, never()).save(any());
        verify(paymentClient, never()).initiatePayment(any());
    }

    // -------------------------------------------------------------------------
    // processCallback — happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processCallback: updates payment to SUCCESS and publishes success event")
    void processCallback_Success_UpdatesStatusAndPublishesEvent() {
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
        verify(eventPublisher).publishPaymentSuccess(any());
    }

    @Test
    @DisplayName("processCallback: updates payment to FAILED and publishes failed event")
    void processCallback_Failed_UpdatesStatusAndPublishesFailedEvent() {
        PaymentCallback callback = buildCallback("FAILED");
        Payment payment = buildPayment("INITIATED");

        when(idempotencyService.isCallbackAlreadyProcessed(txnRef)).thenReturn(false);
        when(paymentRepository.findByTransactionReference(txnRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        paymentService.processCallback(callback);

        verify(eventPublisher).publishPaymentFailed(any());
        verify(eventPublisher, never()).publishPaymentSuccess(any());
    }

    @Test
    @DisplayName("processCallback: returns existing record idempotently for duplicate callback")
    void processCallback_DuplicateCallback_ReturnsExistingIdempotently() {
        PaymentCallback callback = buildCallback("SUCCESS");
        Payment payment = buildPayment("SUCCESS");

        when(idempotencyService.isCallbackAlreadyProcessed(txnRef)).thenReturn(true);
        when(paymentRepository.findByTransactionReference(txnRef)).thenReturn(Optional.of(payment));

        PaymentResponse result = paymentService.processCallback(callback);

        assertThat(result.status()).isEqualTo("SUCCESS");
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishPaymentSuccess(any());
    }

    @Test
    @DisplayName("processCallback: throws PaymentNotFoundException when txnRef is not found")
    void processCallback_UnknownTxnRef_ThrowsNotFoundException() {
        PaymentCallback callback = buildCallback("SUCCESS");

        when(idempotencyService.isCallbackAlreadyProcessed(txnRef)).thenReturn(false);
        when(paymentRepository.findByTransactionReference(txnRef)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processCallback(callback))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

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

    @Test
    @DisplayName("getPaymentByBookingId: returns payment for valid bookingId")
    void getPaymentByBookingId_Found_ReturnsResponse() {
        Payment payment = buildPayment("SUCCESS");
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        PaymentResponse result = paymentService.getPaymentByBookingId(bookingId);

        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("getPaymentsByUserId: returns paginated results")
    void getPaymentsByUserId_ReturnsPaginatedResults() {
        Payment payment = buildPayment("SUCCESS");
        Page<Payment> page = new PageImpl<>(List.of(payment));
        when(paymentRepository.findByUserId(eq(userId), any())).thenReturn(page);

        Page<PaymentResponse> result = paymentService.getPaymentsByUserId(userId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo("SUCCESS");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
        p.setPaymentGateway("MOCK_GATEWAY");
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
                .gatewayName("MOCK_GATEWAY")
                .failureReason(status.equals("FAILED") ? "Insufficient funds" : null)
                .signature("dummy-signature")
                .build();
    }
}
