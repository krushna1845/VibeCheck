package com.krushna.moviebooking.theatre.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cities_name_state", columnNames = {"name", "state"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @NotBlank(message = "City name is required")
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank(message = "State name is required")
    @Size(max = 100)
    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @NotBlank(message = "Country name is required")
    @Size(max = 100)
    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "India";

    @Size(max = 20)
    @Column(name = "pincode", length = 20)
    private String pincode;

    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Theatre> theatres = new ArrayList<>();
}
