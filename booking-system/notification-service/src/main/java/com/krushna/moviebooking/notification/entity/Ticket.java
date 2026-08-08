package com.krushna.moviebooking.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "booking_reference", nullable = false, length = 50)
    private String bookingReference;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "movie_title", nullable = false)
    private String movieTitle;

    @Column(name = "theatre_name", nullable = false)
    private String theatreName;

    @Column(name = "screen_name", nullable = false, length = 100)
    private String screenName;

    @Column(name = "seat_numbers", nullable = false)
    private String seatNumbers;

    @Column(name = "show_date")
    private LocalDate showDate;

    @Column(name = "show_time")
    private LocalTime showTime;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "qr_code_data", nullable = false, columnDefinition = "TEXT")
    private String qrCodeData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.GENERATED;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
