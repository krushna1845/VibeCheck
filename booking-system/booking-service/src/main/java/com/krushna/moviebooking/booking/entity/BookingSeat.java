package com.krushna.moviebooking.booking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_seats_booking_showseat", columnNames = {"booking_id", "show_seat_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Booking reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @NotNull(message = "Show seat reference ID is required")
    @Column(name = "show_seat_id", nullable = false)
    private UUID showSeatId;

    @NotBlank(message = "Seat number is required")
    @Size(max = 20)
    @Column(name = "seat_number", nullable = false, length = 20)
    private String seatNumber;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
