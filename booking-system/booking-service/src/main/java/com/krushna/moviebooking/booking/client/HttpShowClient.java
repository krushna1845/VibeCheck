package com.krushna.moviebooking.booking.client;

import com.krushna.moviebooking.booking.exception.SeatNotFoundException;
import com.krushna.moviebooking.booking.exception.SeatUnavailableException;
import com.krushna.moviebooking.booking.exception.ShowNotFoundException;
import com.krushna.moviebooking.booking.exception.ShowServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Production implementation of {@link ShowClient} communicating via HTTP RestClient
 * to Show Service.
 */
@Slf4j
@Primary
@Component
public class HttpShowClient implements ShowClient {

    private final RestClient restClient;

    public HttpShowClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.show-service.url:http://show-service:8083}") String showServiceUrl,
            @Value("${services.show-service.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${services.show-service.read-timeout-ms:5000}") int readTimeoutMs,
            @Value("${internal.security.secret:" + com.krushna.moviebooking.common.security.InternalAuthConstants.DEFAULT_INTERNAL_SECRET + "}") String internalSecret) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = restClientBuilder
                .baseUrl(showServiceUrl)
                .requestFactory(requestFactory)
                .defaultHeader(com.krushna.moviebooking.common.security.InternalAuthConstants.INTERNAL_SERVICE_HEADER, "booking-service")
                .defaultHeader(com.krushna.moviebooking.common.security.InternalAuthConstants.INTERNAL_SECRET_HEADER, internalSecret)
                .requestInterceptor((request, body, execution) -> {
                    // Propagate caller Authorization header if present in current request context
                    var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String authHeader = attributes.getRequest().getHeader("Authorization");
                        if (authHeader != null && !authHeader.isBlank()) {
                            request.getHeaders().set("Authorization", authHeader);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();

        log.info("Initialized HttpShowClient with baseUrl: {}, connectTimeout: {}ms, readTimeout: {}ms",
                showServiceUrl, connectTimeoutMs, readTimeoutMs);
    }

    public HttpShowClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public boolean existsShow(UUID showId) {
        log.debug("HTTP checking existsShow for showId: {}", showId);
        if (showId == null) {
            return false;
        }
        try {
            var response = restClient.get()
                    .uri("/api/v1/shows/{id}", showId)
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            log.error("Show service error during existsShow({}): {}", showId, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service error checking show existence: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Show service unreachable during existsShow({}): {}", showId, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service is unavailable or timed out: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<ShowDto> getShowById(UUID showId) {
        log.debug("HTTP fetching getShowById for showId: {}", showId);
        if (showId == null) {
            return Optional.empty();
        }
        try {
            ShowDto show = restClient.get()
                    .uri("/api/v1/shows/{id}", showId)
                    .retrieve()
                    .body(ShowDto.class);
            return Optional.ofNullable(show);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.error("Show service error during getShowById({}): {}", showId, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service error fetching show details: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Show service unreachable during getShowById({}): {}", showId, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service is unavailable or timed out: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ShowSeatDto> getShowSeatsByIds(UUID showId, List<UUID> showSeatIds) {
        log.debug("HTTP fetching show seats for showId: {}, seatIds: {}", showId, showSeatIds);
        if (showId == null || showSeatIds == null || showSeatIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ShowSeatResponseDto[] seats = restClient.get()
                    .uri("/api/v1/shows/{showId}/seats", showId)
                    .retrieve()
                    .body(ShowSeatResponseDto[].class);

            if (seats == null || seats.length == 0) {
                return Collections.emptyList();
            }

            Set<UUID> targetIds = new HashSet<>(showSeatIds);
            List<ShowSeatDto> result = new ArrayList<>();
            for (ShowSeatResponseDto seat : seats) {
                if (targetIds.contains(seat.id())) {
                    result.add(new ShowSeatDto(
                            seat.id(),
                            seat.showId() != null ? seat.showId() : showId,
                            seat.seatId(),
                            seat.seatNumber() != null ? seat.seatNumber() : "SEAT-" + seat.id().toString().substring(0, 4),
                            seat.price(),
                            seat.status()
                    ));
                }
            }
            return result;
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Show not found during getShowSeatsByIds: {}", showId);
            throw new ShowNotFoundException(showId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ShowNotFoundException(showId);
            }
            log.error("Show service error during getShowSeatsByIds({}, {}): {}", showId, showSeatIds, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service error fetching show seats: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Show service unreachable during getShowSeatsByIds({}, {}): {}", showId, showSeatIds, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service is unavailable or timed out: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void updateShowSeatsStatus(UUID showId, List<UUID> showSeatIds, String status) {
        log.info("HTTP updating show seats status for showId: {}, seatIds: {}, targetStatus: {}",
                showId, showSeatIds, status);
        if (showId == null || showSeatIds == null || showSeatIds.isEmpty()) {
            return;
        }

        try {
            var requestPayload = new SeatStatusRequestPayload(showSeatIds, status);
            restClient.put()
                    .uri("/api/v1/shows/{showId}/seats/status", showId)
                    .body(requestPayload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully updated seats to {} in show-service for showId: {}", status, showId);
        } catch (HttpClientErrorException.NotFound ex) {
            log.warn("Seats or show not found during seat status update for show {}: {}", showId, showSeatIds);
            throw new SeatNotFoundException(showId, showSeatIds);
        } catch (HttpClientErrorException.Conflict ex) {
            log.warn("Conflict updating seat status for show {} (seat already booked): {}", showId, showSeatIds);
            throw new SeatUnavailableException(showId, showSeatIds);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new SeatNotFoundException(showId, showSeatIds);
            }
            if (ex.getStatusCode().value() == 409) {
                throw new SeatUnavailableException(showId, showSeatIds);
            }
            log.error("Show service error during updateShowSeatsStatus for show {}: {}", showId, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service error updating seat status: " + ex.getMessage(), ex);
        } catch (ResourceAccessException ex) {
            log.error("Show service unreachable during updateShowSeatsStatus for show {}: {}", showId, ex.getMessage());
            throw new ShowServiceUnavailableException("Show service is unavailable or timed out: " + ex.getMessage(), ex);
        }
    }

    private record ShowSeatResponseDto(
            UUID id,
            UUID showId,
            UUID seatId,
            String seatNumber,
            BigDecimal price,
            String status,
            Instant lockExpiration,
            Long version
    ) {}

    private record SeatStatusRequestPayload(
            List<UUID> showSeatIds,
            String status
    ) {}
}
