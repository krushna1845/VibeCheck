# Milestone 33 — Frontend API Contract Matrix

**Date:** 2026-09-29  
**Platform:** VibeCheck Movie Booking Platform  
**Target Milestone:** 33 — Production Application E2E Hardening  
**Scope:** Complete contract specification between Frontend Client and API Gateway  

---

## 1. API Contract Specification Table

| Frontend Action | HTTP Method | Frontend Path | Gateway Route | Backend Service Endpoint | Auth Required | Request DTO | Response DTO | Error Status Handling |
|:---|:---:|:---|:---|:---|:---:|:---|:---|:---|
| **Register User** | `POST` | `/api/v1/auth/register` | `/api/v1/auth/**` | `auth-service:8080/api/v1/auth/register` | No | `RegisterRequest` | `AuthResponse` | `400` (Validation), `409` (Email exists) |
| **Login User** | `POST` | `/api/v1/auth/login` | `/api/v1/auth/**` | `auth-service:8080/api/v1/auth/login` | No | `LoginRequest` | `AuthResponse` | `401` (Invalid credentials), `400` |
| **Refresh Token** | `POST` | `/api/v1/auth/refresh` | `/api/v1/auth/**` | `auth-service:8080/api/v1/auth/refresh` | No | `RefreshTokenRequest` | `AuthResponse` | `401` (Expired / Invalid refresh token) |
| **Get User Profile**| `GET` | `/api/v1/auth/users/{id}` | `/api/v1/auth/**` | `auth-service:8080/api/v1/auth/users/{id}` | **Yes** | None | `UserResponse` | `401`, `404` |
| **Logout User** | `POST` | `/api/v1/auth/logout/{userId}`| `/api/v1/auth/**` | `auth-service:8080/api/v1/auth/logout/{userId}`| **Yes** | None | `Void` (204) | `401`, `404` |
| **List Movies** | `GET` | `/api/v1/movies` | `/api/v1/movies/**` | `movie-service:8081/api/v1/movies` | No | Query: `page`, `size`, `sort` | `Page<MovieResponse>` | `500`, `503` (CB fallback) |
| **Get Movie by ID** | `GET` | `/api/v1/movies/{id}` | `/api/v1/movies/**` | `movie-service:8081/api/v1/movies/{id}` | No | Path: `id` (UUID) | `MovieResponse` | `404`, `503` |
| **Search Movies** | `GET` | `/api/v1/movies/search` | `/api/v1/movies/**` | `movie-service:8081/api/v1/movies/search` | No | Query: `keyword`, `status` | `Page<MovieResponse>` | `500`, `503` |
| **Now Showing** | `GET` | `/api/v1/movies/now-showing`| `/api/v1/movies/**`| `movie-service:8081/api/v1/movies/now-showing` | No | None | `Page<MovieResponse>` | `500`, `503` |
| **List Cities** | `GET` | `/api/v1/cities` | `/api/v1/cities/**` | `theatre-service:8082/api/v1/cities` | No | None | `List<CityResponse>` | `500`, `503` |
| **List Theatres** | `GET` | `/api/v1/theatres` | `/api/v1/theatres/**` | `theatre-service:8082/api/v1/theatres` | No | Query: `page`, `size` | `Page<TheatreResponse>`| `500`, `503` |
| **Theatres by City**| `GET` | `/api/v1/theatres/city/{cityId}`| `/api/v1/theatres/**`| `theatre-service:8082/api/v1/theatres/city/{cityId}`| No | Path: `cityId` (int) | `List<TheatreResponse>`| `404`, `503` |
| **Shows by Movie** | `GET` | `/api/v1/shows/movie/{movieId}`| `/api/v1/shows/**` | `show-service:8083/api/v1/shows/movie/{movieId}` | No | Path: `movieId` (UUID) | `Page<ShowResponse>` | `404`, `503` |
| **Shows by Date** | `GET` | `/api/v1/shows/date/{date}` | `/api/v1/shows/**` | `show-service:8083/api/v1/shows/date/{date}` | No | Path: `date` (YYYY-MM-DD) | `List<ShowResponse>` | `400`, `503` |
| **Get Show Seats** | `GET` | `/api/v1/shows/{showId}/seats` | `/api/v1/shows/**` | `show-service:8083/api/v1/shows/{showId}/seats` | No | Path: `showId` (UUID) | `List<ShowSeatResponse>`| `404`, `503` |
| **Create Booking** | `POST` | `/api/v1/bookings` | `/api/v1/bookings/**` | `booking-service:8084/api/v1/bookings` | **Yes** | `BookingRequest` | `ApiResponse<BookingResponse>` | `400`, `401`, `409` (Seat conflict) |
| **Get Booking** | `GET` | `/api/v1/bookings/{reference}` | `/api/v1/bookings/**` | `booking-service:8084/api/v1/bookings/{reference}` | **Yes** | Path: `reference` (12-char) | `ApiResponse<BookingResponse>` | `401`, `404` |
| **Confirm Booking** | `POST` | `/api/v1/bookings/confirm` | `/api/v1/bookings/**` | `booking-service:8084/api/v1/bookings/confirm` | **Yes** | `PaymentCallbackRequest` | `ApiResponse<BookingResponse>` | `401`, `410` (Expired), `409` |
| **Cancel Booking** | `POST` | `/api/v1/bookings/{ref}/cancel`| `/api/v1/bookings/**` | `booking-service:8084/api/v1/bookings/{ref}/cancel` | **Yes** | Path: `ref`, Query: `reason` | `ApiResponse<BookingResponse>` | `401`, `404`, `409` |
| **User Bookings** | `GET` | `/api/v1/bookings/user/{userId}`| `/api/v1/bookings/**`| `booking-service:8084/api/v1/bookings/user/{userId}` | **Yes** | Path: `userId` (UUID) | `ApiResponse<PagedResponse>` | `401` |
| **Initiate Payment**| `POST` | `/api/v1/payments` | `/api/v1/payments/**` | `payment-service:8085/api/v1/payments` | **Yes** | `PaymentRequest` | `PaymentResponse` | `400`, `401`, `409`, `500` |
| **Get Payment** | `GET` | `/api/v1/payments/{paymentId}` | `/api/v1/payments/**` | `payment-service:8085/api/v1/payments/{paymentId}` | **Yes** | Path: `paymentId` (UUID) | `PaymentResponse` | `401`, `404` |
| **Payment Callback**| `POST` | `/api/v1/payments/callback` | `/api/v1/payments/**` | `payment-service:8085/api/v1/payments/callback` | No* (HMAC) | `PaymentCallback` | `PaymentResponse` | `400` (Bad HMAC), `404` |

---

## 2. Request / Response DTO Structures

### 2.1 Authentication DTOs
```json
// RegisterRequest (POST /api/v1/auth/register)
{
  "email": "user@example.com",
  "password": "Password123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+919876543210",
  "roles": ["ROLE_CUSTOMER"]
}

// LoginRequest (POST /api/v1/auth/login)
{
  "email": "user@example.com",
  "password": "Password123!"
}

// AuthResponse
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "48b6f3c1-...",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": {
    "id": "eb6ee2de-d76c-478a-9431-3ed733c93232",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+919876543210",
    "roles": ["ROLE_CUSTOMER"]
  }
}
```

### 2.2 Booking DTOs
```json
// BookingRequest (POST /api/v1/bookings)
{
  "userId": "eb6ee2de-d76c-478a-9431-3ed733c93232",
  "showId": "f250aaf6-b9b9-4569-9667-bfc29707a1ae",
  "showSeatIds": [
    "086689c5-68a1-499a-b2a9-cce40eef6b10",
    "0a03d619-2fd2-4ded-80d7-7ebd8354faa0"
  ],
  "paymentMethod": "UPI",
  "idempotencyKey": "4c9d5e31-89ab-4123-bcde-0123456789ab"
}

// BookingResponse
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "id": "530cce4e-6eb9-4f5a-89ba-1ba540fba436",
    "bookingReference": "BKFD5C21CBFD",
    "userId": "eb6ee2de-d76c-478a-9431-3ed733c93232",
    "showId": "f250aaf6-b9b9-4569-9667-bfc29707a1ae",
    "totalAmount": 620.00,
    "taxAmount": 90.00,
    "convenienceFee": 30.00,
    "status": "PENDING",
    "expiresAt": "2026-09-29T14:45:00Z",
    "seats": [
      { "showSeatId": "086689c5-...", "seatNumber": "A1", "price": 250.00 },
      { "showSeatId": "0a03d619-...", "seatNumber": "A2", "price": 250.00 }
    ]
  }
}
```

### 2.3 Payment DTOs
```json
// PaymentRequest (POST /api/v1/payments)
{
  "bookingId": "530cce4e-6eb9-4f5a-89ba-1ba540fba436",
  "userId": "eb6ee2de-d76c-478a-9431-3ed733c93232",
  "idempotencyKey": "pay-idem-530cce4e-1",
  "amount": 620.00,
  "currency": "INR",
  "paymentMethod": "UPI",
  "bookingReference": "BKFD5C21CBFD"
}

// PaymentResponse
{
  "paymentId": "7b2c9a1d-4e5f-4123-bcde-9876543210ab",
  "bookingId": "530cce4e-6eb9-4f5a-89ba-1ba540fba436",
  "bookingReference": "BKFD5C21CBFD",
  "idempotencyKey": "pay-idem-530cce4e-1",
  "transactionReference": "TXN-BKFD5C21CBFD-7B2C",
  "status": "INITIATED",
  "amount": 620.00,
  "currency": "INR",
  "paymentMethod": "UPI",
  "redirectUrl": "https://mock-payment-gateway.internal/checkout/BKFD5C21CBFD"
}
```

### 2.4 Confirmation DTO
```json
// PaymentCallbackRequest (POST /api/v1/bookings/confirm)
{
  "bookingReference": "BKFD5C21CBFD",
  "paymentId": "7b2c9a1d-4e5f-4123-bcde-9876543210ab"
}
```

---

## 3. Client Header & Security Standards

1. **Authentication Header**:
   - Protected routes must transmit: `Authorization: Bearer <JWT_ACCESS_TOKEN>`.
2. **Correlation ID**:
   - Client sends or Gateway auto-generates: `X-Correlation-ID: <UUID>`.
3. **Idempotency**:
   - `Idempotency-Key: <UUID>` header sent on `POST /api/v1/bookings` and in `PaymentRequest.idempotencyKey`.
4. **Content Negotiation**:
   - `Content-Type: application/json; charset=UTF-8`
   - `Accept: application/json`
