package com.krushna.moviebooking.movie.repository;

import com.krushna.moviebooking.movie.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {

    Optional<Language> findByCode(String code);

    Optional<Language> findByName(String name);

    boolean existsByCode(String code);
}
