package com.krushna.moviebooking.theatre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;

@SpringBootApplication(exclude = {RedisReactiveAutoConfiguration.class})
public class TheatreApplication {
    public static void main(String[] args) {
        SpringApplication.run(TheatreApplication.class, args);
    }
}
