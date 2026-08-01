package com.krushna.moviebooking.movie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Central application configuration for movie-service.
 * Enables JPA Auditing so that @CreatedDate / @LastModifiedDate fields
 * on Movie entity are auto-populated by Spring Data.
 */
@Configuration
@EnableJpaAuditing
public class MovieConfig implements WebMvcConfigurer {
}
