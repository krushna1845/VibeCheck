package com.krushna.moviebooking.theatre.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "screens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_screens_theatre_name", columnNames = {"theatre_id", "name"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Theatre reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    @NotBlank(message = "Screen name is required")
    @Size(max = 50)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank(message = "Screen type is required")
    @Size(max = 30)
    @Column(name = "screen_type", nullable = false, length = 30)
    @Builder.Default
    private String screenType = "STANDARD";

    @NotNull(message = "Total seats capacity is required")
    @Min(value = 1, message = "Total seats must be greater than zero")
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();
}
