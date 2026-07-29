package com.krushna.moviebooking.booking.client;

import com.krushna.moviebooking.booking.dto.PaymentInitiationRequest;
import com.krushna.moviebooking.booking.dto.PaymentInitiationResponse;

/**
 * Client interface for interacting with Payment Service.
 */
public interface PaymentClient {

    /**
     * Initiates payment with the Payment Service.
     */
    PaymentInitiationResponse initiatePayment(PaymentInitiationRequest request);
}
