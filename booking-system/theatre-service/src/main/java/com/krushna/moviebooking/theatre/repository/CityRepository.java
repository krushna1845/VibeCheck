package com.krushna.moviebooking.theatre.repository;

import com.krushna.moviebooking.theatre.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {

    Optional<City> findByNameIgnoreCaseAndStateIgnoreCase(String name, String state);

    List<City> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndStateIgnoreCase(String name, String state);
}
