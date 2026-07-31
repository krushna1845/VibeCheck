package com.krushna.moviebooking.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * Inbound DTO representing a payment status callback from the external gateway.
 *
 * <p>The payment service uses this payload to reconcile a gateway transaction
 * with an existing internal {@code Payment} record. The {@code transactionReference}
 * field is used as the idempotency guard against duplicate callbacks.
 *
 * @param transactionReference External gateway transaction reference (used for dedup)
 * @param gatewayStatus        Raw status string from the gateway (SUCCESS / FAILED / PENDING)
 * @param gatewayName          Name of the gateway that issued the callback
 * @param failureReason        Optional reason when gatewayStatus is FAILED
 * @param signature            HMAC-SHA256 signature for payload verification
 */
@Builder
public record PaymentCallback(

        @NotBlank(message = "Transaction reference is required")
        @Size(max = 255)
        String transactionReference,

        @NotBlank(message = "Gateway status is required")
        String gatewayStatus,

        @NotBlank(message = "Gateway name is required")
        @Size(max = 50)
        String gatewayName,

        String failureReason,

        @NotNull(message = "Callback signature is required")
        String signature
) {}
