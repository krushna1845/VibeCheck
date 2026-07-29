package com.krushna.moviebooking.booking.client;

import com.krushna.moviebooking.booking.dto.PaymentInitiationRequest;
import com.krushna.moviebooking.booking.dto.PaymentInitiationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Primary component implementation of {@link PaymentClient}.
 */
@Slf4j
@Component
public class DefaultPaymentClient implements PaymentClient {

    @Override
    public PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request) {
        log.info("Initiating payment for bookingReference: {}, amount: {}, method: {}",
                request.bookingReference(), request.amount(), request.paymentMethod());

        UUID paymentId = UUID.randomUUID();
        String txnRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return PaymentInitiationResponse.builder()
                .paymentId(paymentId)
                .bookingReference(request.bookingReference())
                .paymentStatus("INITIATED")
                .redirectUrl("https://payment-gateway.com/checkout/" + paymentId)
                .transactionReference(txnRef)
                .build();
    }
}
