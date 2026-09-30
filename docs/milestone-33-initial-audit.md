# Milestone 33 — Initial Application E2E Audit

**Date:** 2026-09-29  
**Platform:** VibeCheck Movie Booking Platform  
**Target Milestone:** 33 — Production Application E2E Hardening  
**Audience:** Principal Backend Engineer, SRE, Platform Engineer, Frontend Engineer  

---

## 1. Executive Summary

Milestone 32 verified all Kubernetes HA infrastructure, Kafka 3-broker clustering, MySQL/Redis stateful persistence, probes, zero-trust network policies, and CI validation (50 PASS / 0 FAIL). 

However, an exhaustive audit of the customer-facing frontend (`web/`), admin portal (`admin-portal/`), and API Gateway routing demonstrates that **the customer application is currently a client-side simulation (mock)**. The browser client does not execute any HTTP network requests (`fetch` / `XMLHttpRequest`), does not communicate with the API Gateway (`gateway-service`), does not authenticate against `auth-service`, and simulates bookings and payments entirely via `localStorage` and `setTimeout`.

This document identifies all architectural components, API gaps, broken workflows, security vulnerabilities, and deployment integration risks prior to implementing the real runtime integration.

---

## 2. Platform Architecture & Component Inventory

```
[ Customer Browser (web/) ]
             │
             ▼ (HTTP / REST + Bearer JWT)
   [ API Gateway (Port 8079) ] ── (Spring Cloud / Resilience4j / JWT Filter)
             │
   ┌─────────┼───────────────────┬────────────────────┬─────────────────┐
   ▼         ▼                   ▼                    ▼                 ▼
[auth-svc] [movie-svc]      [theatre-svc]        [show-svc]       [booking-svc]
 (8080)     (8081)             (8082)               (8083)           (8084)
                                                                        │
                                                              ┌─────────┴─────────┐
                                                              ▼                   ▼
                                                        [payment-svc]    [notification-svc]
                                                           (8085)              (8086)
```

### Component Details
1. **Frontend Web App (`web/`)**:
   - Vanilla JavaScript (ES6+), semantic HTML5, Vanilla CSS3.
   - Single Page Application (SPA) architecture with view switching (`showPage`).
   - Assets: `web/index.html` (571 lines), `web/css/styles.css` (37KB), `web/js/app.js` (1,159 lines).
2. **Landing Hub (`index.html`)**:
   - Root index file routing users to Customer Web App (`/web/`), Partner Portal (`/partner-portal/`), or Admin Console (`/admin-portal/`).
3. **API Gateway (`gateway-service`, Port 8079)**:
   - Central entry point and reverse proxy with Resilience4j circuit breakers and retries.
   - Enforces JWT authentication, correlation IDs, rate limiting, and internal perimeter header injection (`X-Internal-Secret`, `X-User-Id`, `X-User-Roles`).
4. **Core Domain Services**:
   - `auth-service` (8080): User registration, BCrypt password hashing, HMAC-SHA512 JWT access tokens & refresh tokens.
   - `movie-service` (8081): Movie catalog, genres, languages, search.
   - `theatre-service` (8082): Theatres, screens, seats, cities.
   - `show-service` (8083): Shows, show seats, seat availability status, pricing.
   - `booking-service` (8084): Seat reservation, Redis distributed locking (`seat:{showId}:{seatId}`), booking state machine (`PENDING` -> `CONFIRMED` / `CANCELLED`), transactional outbox.
   - `payment-service` (8085): Payment initiation, multi-provider client (`MOCK`, `RAZORPAY`, `STRIPE`), callback & webhook processing with HMAC-SHA256 signature verification, idempotency.
   - `notification-service` (8086): Tickets, email/SMS notification consumers.

---

## 3. Frontend Routes & Views

| View / Modal ID | UI Name | Component Function | Current Behavior |
|:---|:---|:---|:---|
| `#page-home` | Home Page | Hero banner, filters, movie cards grid, coming soon | Renders static `MOVIES` array from memory |
| `#page-booking` | Booking Stepper | 3-step wizard (Date/Show/Seat, F&B, Payment) | Step 1 uses static `SEAT_LAYOUT`; Step 2 uses static `FOOD_ITEMS`; Step 3 calculates client-side price |
| `#page-bookings` | My Bookings | Tabs: Upcoming, Past, Cancelled | Reads from `localStorage.getItem('vibecheck_bookings')` |
| `#page-profile` | My Profile | Form for Name, Email, Mobile | Reads/writes `localStorage.getItem('vibecheck_user')` |
| `#loginOverlay` | Sign In / Register | Modal: Mobile OTP & Email/Password tabs | Client-side fake: accepts any credentials, creates fake user object |
| `#movieOverlay` | Movie Details | Popup: Synopsis, cast, director, "Book Tickets" | Populates from static `MOVIES` array |
| `#ticketOverlay` | E-Ticket View | Modal: QR code canvas, booking details, Download PNG | Renders canvas QR from client-generated `bkId` |
| `#payOverlay` | Payment Spinner | Fullscreen processing overlay | 2.2-second `setTimeout`, then calls `confirmBooking()` |

---

## 4. Gateway Routing Matrix & Security Boundaries

| Gateway Path | Downstream Service | Downstream URL | Auth Policy |
|:---|:---|:---|:---|
| `/api/v1/auth/**` | `auth-service` | `http://localhost:8080/api/v1/auth/**` | Public for `/register`, `/login`, `/refresh`; Protected for `/logout` |
| `/api/v1/movies/**` | `movie-service` | `http://localhost:8081/api/v1/movies/**` | GET Public; POST/PUT/DELETE `ROLE_ADMIN` |
| `/api/v1/genres/**` | `movie-service` | `http://localhost:8081/api/v1/genres/**` | GET Public; POST/PUT/DELETE `ROLE_ADMIN` |
| `/api/v1/languages/**`| `movie-service` | `http://localhost:8081/api/v1/languages/**`| GET Public; POST/PUT/DELETE `ROLE_ADMIN` |
| `/api/v1/theatres/**`| `theatre-service`| `http://localhost:8082/api/v1/theatres/**`| GET Public; Mutations `ROLE_ADMIN` |
| `/api/v1/screens/**` | `theatre-service`| `http://localhost:8082/api/v1/screens/**` | GET Public; Mutations `ROLE_ADMIN` |
| `/api/v1/seats/**`   | `theatre-service`| `http://localhost:8082/api/v1/seats/**`   | GET Public; Mutations `ROLE_ADMIN` |
| `/api/v1/cities/**`  | `theatre-service`| `http://localhost:8082/api/v1/cities/**`  | GET Public; Mutations `ROLE_ADMIN` |
| `/api/v1/shows/**`   | `show-service`   | `http://localhost:8083/api/v1/shows/**`   | GET Public; Mutations `ROLE_ADMIN` |
| `/api/v1/bookings/**`| `booking-service`| `http://localhost:8084/api/v1/bookings/**`| **Authenticated (`Bearer JWT`)** |
| `/api/v1/payments/**`| `payment-service`| `http://localhost:8085/api/v1/payments/**`| **Authenticated (`Bearer JWT`)** except `/callback` & `/webhooks/**` |
| `/api/v1/tickets/**` | `notification-service` | `http://localhost:8086/api/v1/tickets/**` | **Authenticated (`Bearer JWT`)** |

---

## 5. Detailed Gap Analysis & Known Broken Flows

### 5.1 Authentication Flow (CRITICAL GAP)
* **Frontend Implementation**:
  - `emailLogin()`: parses user name from email string (`email.split('@')[0]`), generates fake user object `{ name, email, avatar }`, stores in `localStorage`, and updates UI.
  - `emailSignup()`: checks `pw.length >= 6`, then immediately calls `loginUser()`.
  - `verifyOTP()`: accepts any 6-digit number and logs in as "User".
  - `logout()`: simply deletes `localStorage` key.
* **Backend Expectation**:
  - `POST /api/v1/auth/register` requires `{ email, password, firstName, lastName, phoneNumber }`.
  - `POST /api/v1/auth/login` requires `{ email, password }`, returning `{ accessToken, refreshToken, tokenType, user }`.
* **Impact**: No JWT token is ever obtained or stored. Any downstream call requiring authentication will immediately receive **HTTP 401 Unauthorized** from API Gateway.

### 5.2 Movie Catalog & Show Scheduling (DISCONNECTED)
* **Frontend Implementation**: Uses hardcoded `MOVIES = [...]` (8 movies) with fixed string IDs (0..7).
* **Backend Expectation**:
  - `GET /api/v1/movies`: returns real database movies with UUID primary keys.
  - `GET /api/v1/shows/movie/{movieId}`: returns scheduled show instances with UUID screen and theatre links.
* **Impact**: Customer cannot see newly created backend movies or shows; frontend IDs cannot be used to book backend shows.

### 5.3 Seat Map & Real-Time Availability (MOCK ONLY)
* **Frontend Implementation**: Uses static matrix `SEAT_LAYOUT` (rows A-J, numbers 1-10) with hardcoded available/booked states.
* **Backend Expectation**:
  - `GET /api/v1/shows/{showId}/seats`: returns actual show seats with UUID `showSeatId`, tier pricing (`RECLINER`, `PRIME`, `CLASSIC`), and real-time status (`AVAILABLE`, `LOCKED`, `BOOKED`).
* **Impact**: No Redis seat locking is triggered; two users can select the same seats without conflict detection.

### 5.4 Booking Creation (BYPASSED)
* **Frontend Implementation**: Never calls backend. Directly synthesizes a local ticket ID (`VC...`) in `confirmBooking()`.
* **Backend Expectation**:
  - `POST /api/v1/bookings` with payload `{ userId, showId, showSeatIds, paymentMethod, idempotencyKey }`.
  - Acquires Redis distributed lock with TTL.
  - Creates database entity with status `PENDING` and returns 12-character `bookingReference`.
* **Impact**: Zero bookings are created in `vibecheck_booking` database; transactional outbox and Kafka events never fire.

### 5.5 Payment Processing (CRITICAL SECURITY RISK)
* **Frontend Implementation**:
  - `processPayment()` sets a 2.2s timer and directly calls `confirmBooking()`.
  - Does NOT initiate payment with `payment-service`.
  - Does NOT handle payment callbacks, webhooks, or signatures.
* **Backend Expectation**:
  - `POST /api/v1/payments` with `{ bookingId, userId, amount, paymentMethod, bookingReference, idempotencyKey }`.
  - Verifies payment with gateway client (`MOCK` / `RAZORPAY` / `STRIPE`).
  - Upon callback (`POST /api/v1/payments/callback`) or webhook, updates payment status and triggers `POST /api/v1/bookings/confirm`.
* **Impact**: The UI marks bookings as confirmed without financial verification or payment state persistence.

---

## 6. Environment & Configuration Audit

1. **Hardcoded URLs**:
   - `web/js/app.js` contains hardcoded Unsplash image URLs for posters/banners, but zero API URLs.
   - `admin-portal/js/admin.js` has hardcoded fallback `http://localhost:8080` (which is `auth-service`, not the gateway!).
2. **Missing API Client & Config Layer**:
   - There is no central configuration file (e.g. `api-config.js` or `api.js`) defining `GATEWAY_URL`.
   - When deploying to Minikube or Production behind Kubernetes Ingress, the frontend needs to make requests either to the origin host `/api/v1` or to the configured gateway hostname.
3. **CORS Configuration**:
   - `gateway-service/src/main/resources/application.yml` specifies `cors.allowed-origins: "*"`.
   - In `SecurityConfig.java`:
     ```java
     if ("*".equals(corsAllowedOrigins.trim())) {
         config.setAllowedOriginPatterns(List.of("*"));
     }
     config.setAllowCredentials(true);
     ```
   - Using `setAllowedOriginPatterns(["*"])` allows credentials (`Authorization` header) from browser clients. However, when served from `file://` or non-standard dev origins, CORS preflight must be tested.

---

## 7. Deployment Integration Risks

1. **Frontend Hosting in Production / Kubernetes**:
   - `ingress.yaml` routes `/` directly to `gateway-service:8079`.
   - `gateway-service` is a Spring Boot application that currently has no static file resources in `src/main/resources/static`.
   - If a browser navigates to `http://api.vibecheck.com/`, the gateway returns HTTP 404 or 401 because it does not host the `web/` assets.
   - **Resolution Required**: Either configure `gateway-service` to serve static web assets, or introduce a static web container / Nginx pod in Helm, or configure ingress routing so that `/` serves static files and `/api/v1` routes to `gateway-service`.
2. **Token Lifecycle on Browser Refresh**:
   - In a production SPA, browser refresh should check `localStorage` for an active `accessToken` and validate or refresh it against `/api/v1/auth/validate` or `/refresh`.

---

## 8. Action Plan for Milestone 33 Hardening

1. **Phase 1: API Contract Matrix** — Document every API contract between frontend and backend.
2. **Phase 2: Authentication Hardening** — Implement real register/login/refresh/logout in frontend with JWT storage and central 401 interceptor.
3. **Phase 3: Movie -> Show -> Seat Flow** — Connect frontend to real catalog APIs, dynamic showtimes, and show seat maps.
4. **Phase 4: Booking Flow** — Connect seat reservation to `POST /api/v1/bookings`, handle Redis lock conflict (409).
5. **Phase 5: Payment Flow** — Connect payment to `POST /api/v1/payments`, implement verified callback/confirmation loop.
6. **Phase 6: UI Action Matrix** — Verify and harden all buttons, modals, and forms.
7. **Phase 7: Error Handling & UX** — Central toast/modal error alerts, loading spinners, and disabled button states.
8. **Phase 8: Environment Config** — Provide flexible `config.js` supporting local, Docker Compose, and Kubernetes.
9. **Phase 9: CORS & Gateway Verification** — Verify preflight and header propagation.
10. **Phase 10: Runtime E2E Verification** — Execute live end-to-end customer journey.
11. **Phase 11-13: Production Build, Regression Tests & Final Report**.
