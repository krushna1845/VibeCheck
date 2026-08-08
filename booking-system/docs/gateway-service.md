# API Gateway Service — Technical Documentation

**Milestone 5 | gateway-service**

---

## 1. Overview & Architecture

The `gateway-service` acts as the single entry point for all client requests in the Movie Booking Platform. Built on Spring MVC with a RestTemplate reverse-proxy engine, it provides JWT authentication, distributed request tracing via Correlation IDs, Resilience4j fault tolerance (Circuit Breakers, Retries, Timeouts, Fallbacks), Micrometer operational metrics, global CORS management, and comprehensive health monitoring.

```mermaid
graph TD
    Client[Browser / Mobile / API Client] -->|1. Request with Bearer JWT| GW[gateway-service :8079]

    subgraph gateway-service
        CID[CorrelationIdFilter] --> RLF[GatewayRequestLoggingFilter]
        RLF --> JWT[GatewayJwtFilter]
        JWT --> RHF[ResponseHeaderFilter]
        RHF --> PC[ProxyController]

        PC -->|Decorate with CB & Retry| R4J[Resilience4j Engine]
        R4J -->|Failure / Timeout / Circuit OPEN| FC[FallbackController]
        R4J -->|Success| RT[RestTemplate Forwarder]
    end

    RT -->|2. Forward + X-Correlation-ID + X-User-Id| AUTH[auth-service :8080]
    RT -->|2. Forward| MOVIE[movie-service :8081]
    RT -->|2. Forward| THEATRE[theatre-service :8082]
    RT -->|2. Forward| SHOW[show-service :8083]
    RT -->|2. Forward| BOOKING[booking-service :8084]
    RT -->|2. Forward| PAYMENT[payment-service :8085]

    FC -->|3. Fallback Response (HTTP 503)| Client
```

---

## 2. Reverse Proxy Route Table

| Downstream Service | Port | Gateway Prefix Path | Downstream Target URL | Fallback Endpoint |
|---|---|---|---|---|
| `auth-service` | 8080 | `/api/v1/auth/**` | `http://localhost:8080/api/v1/auth/**` | `/fallback/auth` |
| `movie-service` | 8081 | `/api/v1/movies/**` | `http://localhost:8081/api/v1/movies/**` | `/fallback/movie` |
| `theatre-service` | 8082 | `/api/v1/theatres/**` | `http://localhost:8082/api/v1/theatres/**` | `/fallback/theatre` |
| `show-service` | 8083 | `/api/v1/shows/**` | `http://localhost:8083/api/v1/shows/**` | `/fallback/show` |
| `booking-service` | 8084 | `/api/v1/bookings/**` | `http://localhost:8084/api/v1/bookings/**` | `/fallback/booking` |
| `payment-service` | 8085 | `/api/v1/payments/**` | `http://localhost:8085/api/v1/payments/**` | `/fallback/payment` |

---

## 3. Resilience4j Fault Tolerance

### A. Circuit Breaker Configuration

- **Sliding Window Type**: `COUNT_BASED`
- **Sliding Window Size**: 10 calls
- **Minimum Calls Threshold**: 5 calls
- **Failure Rate Threshold**: 50%
- **Slow Call Duration Threshold**: 3000 ms
- **Wait Duration in OPEN state**: 5000 ms (5 seconds)
- **Permitted Calls in HALF_OPEN**: 3 calls
- **Automatic Transition to HALF_OPEN**: Enabled

### B. Retry Policy

- **Max Attempts**: 3
- **Wait Duration**: 500 ms
- **Exponential Backoff Multiplier**: 2.0 (500ms -> 1000ms -> 2000ms)

### C. TimeLimiter (Timeouts)

- **Timeout Duration**: 3.0 seconds
- **Cancel Running Future**: Enabled

---

## 4. Fallback Endpoints

When a downstream service is unreachable, returns HTTP 5xx errors, times out, or when its circuit breaker is in the `OPEN` state, `ProxyController` automatically routes execution to the corresponding fallback endpoint.

### Fallback Response Schema (HTTP 503 Service Unavailable)

```json
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Booking Service is currently overloaded. Your request could not be processed at this time.",
  "service": "booking-service",
  "timestamp": "2026-08-08T23:40:00Z"
}
```

---

## 5. Correlation ID & Distributed Tracing

1. **Extraction / Generation**:
   - `CorrelationIdFilter` inspects incoming request for header `X-Correlation-ID`.
   - If missing, generates a unique UUID (e.g. `c7a10f82-4112-4d1a-b605-891000000042`).
2. **MDC Population**:
   - Populates SLF4J MDC with key `correlationId` so all log statements include the request context.
3. **Downstream Header Injection**:
   - `ProxyController` forwards `X-Correlation-ID` to target downstream services.
   - Also injects authenticated user details: `X-User-Id` and `X-User-Roles`.
4. **Client Response Header**:
   - Every gateway HTTP response includes `X-Correlation-ID`.

---

## 6. Request Logging & Security Headers

### Gateway Request Logs

```text
23:40:00.123 [main] INFO  c.k.m.g.f.GatewayRequestLoggingFilter - [GatewayRequest] INCOMING METHOD=GET URI=/api/v1/movies/active CLIENT_IP=127.0.0.1 CORRELATION_ID=c7a10f82-...
23:40:00.145 [main] INFO  c.k.m.g.f.GatewayRequestLoggingFilter - [GatewayResponse] COMPLETED METHOD=GET URI=/api/v1/movies/active STATUS=200 DURATION=22ms CORRELATION_ID=c7a10f82-...
```

### Injected Response Headers

| Header Name | Example Value | Purpose |
|---|---|---|
| `X-Correlation-ID` | `c7a10f82-4112-4d1a-b605-891000000042` | End-to-end request tracing ID |
| `X-Gateway-Timestamp` | `2026-08-08T23:40:00Z` | Response timestamp |
| `X-Gateway-Server` | `MovieBooking-Gateway/1.0` | Gateway identifier |
| `X-Content-Type-Options` | `nosniff` | Prevents MIME sniffing |
| `X-Frame-Options` | `DENY` | Prevents Clickjacking |
| `X-XSS-Protection` | `1; mode=block` | XSS protection |

---

## 7. CORS Configuration

- **Allowed Origin Patterns**: `*` (configurable via `cors.allowed-origins`)
- **Allowed HTTP Methods**: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- **Allowed Headers**: `*`
- **Exposed Response Headers**: `X-Correlation-ID`, `X-Gateway-Timestamp`, `Authorization`
- **Allow Credentials**: `true`
- **Max Age**: 3600 seconds (1 hour pre-flight cache)

---

## 8. Micrometer Metrics & Health Monitoring

### Operational Metrics Registered

| Metric Name | Tags | Description |
|---|---|---|
| `gateway.requests.total` | `service`, `status` | Counter tracking total requests per downstream service and status code |
| `gateway.requests.duration` | `service`, `status` | Timer measuring latency distribution |
| `gateway.fallback.total` | `service`, `reason` | Counter tracking fallback trigger count |

### Management & Health Endpoints

- **Gateway Health Check**: `GET /gateway/health`
  - Returns gateway status, service name, timestamp, and real-time state of all 6 Resilience4j circuit breakers (`CLOSED`, `OPEN`, `HALF_OPEN`).
- **Actuator Health Check**: `GET /actuator/health`
- **Prometheus Metrics**: `GET /actuator/prometheus`
- **Circuit Breaker Actuator**: `GET /actuator/circuitbreakers`

---

## 9. Verification & Tests

Executed via Maven:

```bash
mvn test -pl gateway-service
```

### Test Suites Included

1. `CorrelationIdFilterTest`: Tests header extraction, generation, MDC propagation, and response header.
2. `ResponseHeaderFilterTest`: Tests timestamp, server, and security header injection.
3. `FallbackControllerTest`: Tests fallback responses for all 6 downstream services.
4. `GatewayControllerTest`: Tests `/gateway/health`, `/gateway/routes`, and `/gateway/introspect`.
5. `GatewayIntegrationTest`: End-to-end test of proxy forwarding, circuit breaker fallbacks, CORS pre-flight, and actuator health checks.
