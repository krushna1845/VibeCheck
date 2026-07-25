package com.krushna.moviebooking.theatre.repository;

import com.krushna.moviebooking.theatre.entity.Theatre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, UUID> {

    Page<Theatre> findByCityIdAndStatus(Integer cityId, String status, Pageable pageable);

    List<Theatre> findByCityId(Integer cityId);

    Page<Theatre> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
