package com.krushna.moviebooking.show.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shows")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Movie reference ID is required")
    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @NotNull(message = "Theatre reference ID is required")
    @Column(name = "theatre_id", nullable = false)
    private UUID theatreId;

    @NotNull(message = "Screen reference ID is required")
    @Column(name = "screen_id", nullable = false)
    private UUID screenId;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @NotBlank(message = "Language is required")
    @Size(max = 50)
    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShowSeat> showSeats = new ArrayList<>();
}
