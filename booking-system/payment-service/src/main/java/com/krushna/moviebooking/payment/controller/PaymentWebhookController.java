package com.krushna.moviebooking.payment.controller;

import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for receiving external gateway webhooks (e.g. Razorpay, Stripe) with raw payload and headers.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay")
    public ResponseEntity<PaymentResponse> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers) {
        log.info("[WebhookController] Received Razorpay webhook");
        PaymentResponse response = paymentService.processWebhook("RAZORPAY", rawPayload, headers);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe")
    public ResponseEntity<PaymentResponse> handleStripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader Map<String, String> headers) {
        log.info("[WebhookController] Received Stripe webhook");
        PaymentResponse response = paymentService.processWebhook("STRIPE", rawPayload, headers);
        return ResponseEntity.ok(response);
    }
}
