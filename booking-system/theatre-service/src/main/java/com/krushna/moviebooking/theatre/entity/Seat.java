package com.krushna.moviebooking.theatre.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_seats_screen_row_num", columnNames = {"screen_id", "seat_row", "seat_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Screen reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @NotBlank(message = "Seat row is required")
    @Size(max = 10)
    @Column(name = "seat_row", nullable = false, length = 10)
    private String seatRow;

    @NotNull(message = "Seat number is required")
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @NotBlank(message = "Seat category is required")
    @Size(max = 30)
    @Column(name = "seat_category", nullable = false, length = 30)
    private String seatCategory;

    @NotNull
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
