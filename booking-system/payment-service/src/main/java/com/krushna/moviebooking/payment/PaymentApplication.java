package com.krushna.moviebooking.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry-point for the Payment Service.
 *
 * <p>Provides idempotent payment initiation, external gateway integration,
 * callback handling with duplicate protection, retry support, and structured
 * transactional correctness.
 */
@SpringBootApplication
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
