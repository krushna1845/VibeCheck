package com.krushna.moviebooking.payment.controller;

import com.krushna.moviebooking.payment.dto.PaymentResponse;
import com.krushna.moviebooking.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payment Webhooks", description = "Inbound webhook processing for external payment gateways (Razorpay, Stripe)")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @Operation(summary = "Razorpay Webhook Endpoint", description = "Processes asynchronous webhook events from Razorpay payment gateway.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Razorpay webhook processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid webhook signature or payload")
    })
    @PostMapping("/razorpay")
    public ResponseEntity<PaymentResponse> handleRazorpayWebhook(
            @Parameter(description = "Raw JSON payload body", required = true) @RequestBody String rawPayload,
            @Parameter(description = "Inbound HTTP headers including signature") @RequestHeader Map<String, String> headers) {
        log.info("[WebhookController] Received Razorpay webhook");
        PaymentResponse response = paymentService.processWebhook("RAZORPAY", rawPayload, headers);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Stripe Webhook Endpoint", description = "Processes asynchronous webhook events from Stripe payment gateway.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stripe webhook processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid webhook signature or payload")
    })
    @PostMapping("/stripe")
    public ResponseEntity<PaymentResponse> handleStripeWebhook(
            @Parameter(description = "Raw JSON payload body", required = true) @RequestBody String rawPayload,
            @Parameter(description = "Inbound HTTP headers including signature") @RequestHeader Map<String, String> headers) {
        log.info("[WebhookController] Received Stripe webhook");
        PaymentResponse response = paymentService.processWebhook("STRIPE", rawPayload, headers);
        return ResponseEntity.ok(response);
    }
}

