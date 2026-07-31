package com.krushna.moviebooking.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 configuration for Booking Service.
 *
 * <p>Exposes Swagger UI at {@code /swagger-ui.html} and the OpenAPI spec at
 * {@code /v3/api-docs}. All endpoints require a Bearer JWT token documented
 * under the {@code bearerAuth} security scheme.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI bookingServiceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .components(securityComponents())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Info apiInfo() {
        return new Info()
                .title("Booking Service API")
                .description("""
                        RESTful API for the Movie Booking Platform – Booking Service.
                        
                        Provides endpoints for:
                        - **Booking lifecycle**: create, confirm, cancel, expire, soft-delete
                        - **Booking queries**: by reference, by ID, user history, show occupancy
                        - **Admin operations**: search with filtering / sorting / pagination, force status update
                        
                        All responses follow RFC 7807 Problem Details for error payloads.
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Krushna Movie Booking Platform")
                        .email("support@krushna.com")
                        .url("https://github.com/krushna1845/VibeCheck"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private List<Server> servers() {
        return List.of(
                new Server().url("http://localhost:8082").description("Local Development"),
                new Server().url("https://api.krushna.com/booking").description("Production")
        );
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Provide a valid JWT token obtained from the Auth Service."));
    }
}
