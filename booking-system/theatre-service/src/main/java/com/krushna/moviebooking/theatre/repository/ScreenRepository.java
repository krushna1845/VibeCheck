package com.krushna.moviebooking.theatre.repository;

import com.krushna.moviebooking.theatre.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {

    List<Screen> findByTheatreId(UUID theatreId);

    Optional<Screen> findByTheatreIdAndName(UUID theatreId, String name);

    Optional<Screen> findByTheatreIdAndNameIgnoreCase(UUID theatreId, String name);

    boolean existsByTheatreIdAndNameIgnoreCase(UUID theatreId, String name);
}
