package com.krushna.moviebooking.gateway.controller;

import com.krushna.moviebooking.gateway.filter.CorrelationIdFilter;
import com.krushna.moviebooking.gateway.service.GatewayMetricsService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Reverse Proxy Controller implementing dynamic request forwarding to downstream services.
 * Integrates Resilience4j Circuit Breakers, Retries, Timeouts, and Fallbacks.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ProxyController {

    private final RestTemplate restTemplate;
    private final GatewayMetricsService metricsService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final FallbackController fallbackController;

    @Value("${services.auth-url:http://localhost:8080}")
    private String authServiceUrl;

    @Value("${services.movie-url:http://localhost:8081}")
    private String movieServiceUrl;

    @Value("${services.theatre-url:http://localhost:8082}")
    private String theatreServiceUrl;

    @Value("${services.show-url:http://localhost:8083}")
    private String showServiceUrl;

    @Value("${services.booking-url:http://localhost:8084}")
    private String bookingServiceUrl;

    @Value("${services.payment-url:http://localhost:8085}")
    private String paymentServiceUrl;

    public ProxyController(RestTemplate restTemplate,
                           GatewayMetricsService metricsService,
                           CircuitBreakerRegistry circuitBreakerRegistry,
                           RetryRegistry retryRegistry,
                           FallbackController fallbackController) {
        this.restTemplate = restTemplate;
        this.metricsService = metricsService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.fallbackController = fallbackController;
    }

    @RequestMapping("/auth/**")
    public ResponseEntity<?> proxyAuth(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxyRequest("authService", authServiceUrl, "/api/v1/auth", request, body, fallbackController::authFallback);
    }

    @RequestMapping({"/movies/**", "/genres/**", "/languages/**"})
    public ResponseEntity<?> proxyMovies(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxyRequest("movieService", movieServiceUrl, "/api/v1/movies", request, body, fallbackController::movieFallback);
    }

    @RequestMapping({"/theatres/**", "/screens/**", "/seats/**", "/cities/**"})
    public ResponseEntity<?> proxyTheatres(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxyRequest("theatreService", theatreServiceUrl, "/api/v1/theatres", request, body, fallbackController::theatreFallback);
    }

    @RequestMapping("/shows/**")
    public ResponseEntity<?> proxyShows(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxyRequest("showService", showServiceUrl, "/api/v1/shows", request, body, fallbackController::showFallback);
    }

    @RequestMapping("/bookings/**")
    public ResponseEntity<?> proxyBookings(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxyRequest("bookingService", bookingServiceUrl, "/api/v1/bookings", request, body, fallbackController::bookingFallback);
    }

    @RequestMapping("/payments/**")
    public ResponseEntity<?> proxyPayments(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        return proxyRequest("paymentService", paymentServiceUrl, "/api/v1/payments", request, body, fallbackController::paymentFallback);
    }

    private ResponseEntity<?> proxyRequest(String instanceName,
                                            String baseUrl,
                                            String prefix,
                                            HttpServletRequest request,
                                            byte[] body,
                                            Callable<ResponseEntity<Map<String, Object>>> fallbackCallable) {
        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String targetUrl = baseUrl + path + (queryString != null ? "?" + queryString : "");

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = extractAndEnrichHeaders(request);

        log.debug("[GatewayProxy] Forwarding {} request to {}", method, targetUrl);

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
        Retry retry = retryRegistry.retry(instanceName);

        Callable<ResponseEntity<byte[]>> decoratedCall = CircuitBreaker.decorateCallable(circuitBreaker,
                Retry.decorateCallable(retry, () -> {
                    HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
                    return restTemplate.exchange(URI.create(targetUrl), method, entity, byte[].class);
                }));

        try {
            ResponseEntity<byte[]> response = decoratedCall.call();
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRequest(instanceName, response.getStatusCode().value(), duration);
            return response;
        } catch (HttpStatusCodeException ex) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRequest(instanceName, ex.getStatusCode().value(), duration);
            log.warn("[GatewayProxy] Downstream HTTP error {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode())
                    .headers(filterResponseHeaders(ex.getResponseHeaders()))
                    .body(ex.getResponseBodyAsByteArray());
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRequest(instanceName, 503, duration);
            log.error("[GatewayProxy] Exception executing request to {}: {}", instanceName, ex.getMessage());
            try {
                return fallbackCallable.call();
            } catch (Exception e) {
                return fallbackController.defaultFallback();
            }
        }
    }

    private HttpHeaders extractAndEnrichHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length")) {
                    Enumeration<String> values = request.getHeaders(name);
                    while (values.hasMoreElements()) {
                        headers.add(name, values.nextElement());
                    }
                }
            }
        }

        // Add Correlation ID
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            headers.set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }

        // Inject User Info from SecurityContext if authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            headers.set("X-User-Id", auth.getPrincipal().toString());
            var authorities = auth.getAuthorities().stream().map(Object::toString).toList();
            headers.set("X-User-Roles", String.join(",", authorities));
        }

        return headers;
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders original) {
        if (original == null) return new HttpHeaders();
        HttpHeaders filtered = new HttpHeaders();
        original.forEach((k, v) -> {
            if (!k.equalsIgnoreCase("Transfer-Encoding")) {
                filtered.put(k, v);
            }
        });
        return filtered;
    }
}
