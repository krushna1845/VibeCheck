package com.krushna.moviebooking.theatre.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI theatreServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Theatre Service API")
                        .description("REST API for managing theatres, screens, seats, and cities")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Movie Booking Platform")
                                .email("support@moviebooking.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
