package com.krushna.moviebooking.theatre.repository;

import com.krushna.moviebooking.theatre.entity.Theatre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, UUID> {

    Optional<Theatre> findByIdAndDeletedAtIsNull(UUID id);

    Page<Theatre> findByDeletedAtIsNull(Pageable pageable);

    List<Theatre> findByCityIdAndDeletedAtIsNull(Integer cityId);

    Page<Theatre> findByCityIdAndStatusAndDeletedAtIsNull(Integer cityId, String status, Pageable pageable);

    Page<Theatre> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    Optional<Theatre> findByCityIdAndNameIgnoreCaseAndDeletedAtIsNull(Integer cityId, String name);

    boolean existsByCityIdAndNameIgnoreCaseAndDeletedAtIsNull(Integer cityId, String name);
}
