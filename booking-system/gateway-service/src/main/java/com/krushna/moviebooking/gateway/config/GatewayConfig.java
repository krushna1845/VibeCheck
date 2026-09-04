package com.krushna.moviebooking.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GatewayConfig {

    @Bean
    public RestTemplate restTemplate(
            @org.springframework.beans.factory.annotation.Value("${gateway.http.connect-timeout-ms:3000}") int connectTimeout,
            @org.springframework.beans.factory.annotation.Value("${gateway.http.read-timeout-ms:5000}") int readTimeout) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    @Bean
    public OpenAPI gatewayServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway Service")
                        .description("Central API Gateway for the Movie Booking Platform — JWT validation, routing, rate limiting")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Movie Booking Platform")
                                .email("support@moviebooking.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
