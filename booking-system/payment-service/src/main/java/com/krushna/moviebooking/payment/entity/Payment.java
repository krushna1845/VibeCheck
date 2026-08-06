package com.krushna.moviebooking.payment.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Booking reference ID is required")
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @NotNull(message = "User reference ID is required")
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 255)
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @NotBlank(message = "Payment gateway name is required")
    @Size(max = 50)
    @Column(name = "payment_gateway", nullable = false, length = 50)
    private String paymentGateway;

    @Size(max = 255)
    @Column(name = "transaction_reference")
    private String transactionReference;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Size(max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Size(max = 50)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Size(max = 255)
    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;

    @Size(max = 255)
    @Column(name = "gateway_order_id")
    private String gatewayOrderId;

    @Size(max = 255)
    @Column(name = "refund_reference")
    private String refundReference;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Size(max = 50)
    @Column(name = "refund_status", length = 50)
    private String refundStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
