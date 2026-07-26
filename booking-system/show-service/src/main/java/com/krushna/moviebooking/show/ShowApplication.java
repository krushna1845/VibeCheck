package com.krushna.moviebooking.show;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ShowApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShowApplication.class, args);
    }
}
