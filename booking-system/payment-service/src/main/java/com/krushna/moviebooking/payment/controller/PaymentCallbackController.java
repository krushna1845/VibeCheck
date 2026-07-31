package com.krushna.moviebooking.payment.controller;

import com.krushna.moviebooking.payment.dto.PaymentCallback;
import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dedicated controller for inbound payment gateway callbacks.
 *
 * <p>Separated from {@link PaymentController} to apply a distinct security policy:
 * this endpoint is permit-all (unauthenticated) because the external gateway cannot
 * present a Bearer token. Security is instead enforced by HMAC-SHA256 signature
 * verification inside the service layer.
 *
 * <p>Duplicate callbacks for the same {@code transactionReference} are handled
 * idempotently — the same {@link PaymentResponse} is returned without re-processing.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentService paymentService;

    /**
     * Receives an inbound gateway callback and reconciles it with the existing payment record.
     *
     * <p>HMAC-SHA256 signature is verified before any state mutation occurs.
     * Duplicate callbacks for the same {@code transactionReference} are detected via Redis
     * and returned idempotently.
     *
     * @param callback Validated inbound callback payload from the payment gateway
     * @return 200 OK with the updated {@link PaymentResponse}
     */
    @PostMapping("/callback")
    public ResponseEntity<PaymentResponse> handleCallback(@Valid @RequestBody PaymentCallback callback) {
        log.info("[CallbackController] Received callback | txnRef={} status={} gateway={}",
                callback.transactionReference(), callback.gatewayStatus(), callback.gatewayName());
        PaymentResponse response = paymentService.processCallback(callback);
        log.info("[CallbackController] Callback processed | txnRef={} resultStatus={}",
                callback.transactionReference(), response.status());
        return ResponseEntity.ok(response);
    }
}
