# Milestone 34 — Initial Browser, Authentication & Payment Audit

**Audit Date**: 2026-09-30  
**Auditor**: Senior Full-Stack & Production QA Engineer  
**Target Application**: VibeCheck Movie Booking Platform  

---

## 1. Current Frontend Architecture

The frontend is a Single-Page Application (SPA) located in `web/`:
- **Markup & Structure**: `web/index.html` containing multi-page sections (`#page-home`, `#page-booking`, `#page-bookings`, `#page-profile`), modal dialogs (Login, Movie Detail, E-Ticket), and booking step containers.
- **Styling**: `web/css/styles.css` with responsive layout rules and modern dark/light UI tokens.
- **Configuration**: `web/js/config.js` resolving the API Gateway URL dynamically with window environment overrides, localStorage overrides, and fallback to `http://localhost:8079`.
- **API Client**: `web/js/api.js` providing centralized HTTP fetching (`fetchWithAuth`), JWT token management, 401 token auto-refresh queue, request timeout (12s), and correlation ID (`X-Correlation-ID`) propagation.
- **Application Logic**: `web/js/app.js` managing state (`currentUser`, `selectedMovie`, `selectedShow`, `selectedSeats`, `currentBooking`), catalog loading, seat map rendering, booking orchestration, payment processing, ticket QR drawing, and session events.
- **Containerization**: `web/Dockerfile` and `web/nginx.conf` exposing the static application via Nginx with `/api/` reverse-proxied to `gateway-service:8079`.

---

## 2. Current Authentication Flow

- **Registration (`emailSignup`)**:
  - Endpoint: `POST /api/v1/auth/register` via Gateway.
  - Payload: `{ email, password, firstName, lastName, phoneNumber, roles: ['ROLE_CUSTOMER'] }`.
  - Behavior: Real call made. However, lines 1346–1353 had a fallback client-side synthetic user object if the response was non-standard.
- **Login (`emailLogin`)**:
  - Endpoint: `POST /api/v1/auth/login` via Gateway.
  - Payload: `{ email, password }`.
  - Behavior: Issues real JWT tokens. However, lines 1293–1298 had a fallback synthetic user object.
- **Mobile OTP Login (`verifyOTP`)**:
  - Lines 1255–1272 in `app.js` bypassed backend authentication completely and hardcoded a synthetic customer (`id: 'eb6ee2de-d76c-478a-9431-3ed733c93232'`). This is a critical defect requiring removal or clean redirection to real authentication.
- **Social Login (`socialLogin`)**:
  - Placeholder calling `loginUser()` directly without OAuth2 provider integration.

---

## 3. Current Token Storage

- Access token stored in `localStorage` under key `vibecheck_token`.
- Refresh token stored in `localStorage` under key `vibecheck_refresh_token`.
- User profile stored under `vibecheck_user`.
- Storage is managed centrally by `window.VibeCheckApi.Storage` with `getAccessToken()`, `getRefreshToken()`, `saveAuth()`, and `clearAuth()`.
- Tokens are properly cleared upon logout or session expiration.

---

## 4. Current Refresh-Token Behavior

- Managed by `fetchWithAuth()` in `web/js/api.js`.
- Intercepts HTTP 401 Unauthorized for protected endpoints.
- If a refresh token is present, it pauses concurrent requests and enqueues them in `refreshQueue`.
- Issues `POST /api/v1/auth/refresh` with `{ refreshToken }`.
- On refresh success: Updates local tokens, drains queue, and retries the original request once (`isRetry = true`).
- On refresh failure: Clears authentication storage, dispatches `vibecheck:session_expired`, and throws an explicit `ApiError`.
- Prevents infinite retry loops via `isRetry` guard and route exclusion (`/auth/login`, `/auth/register`).

---

## 5. Current API Gateway Routing

- Gateway (`gateway-service` on port `8079`) provides reverse-proxy routing via `ProxyController.java`:
  - `/api/v1/auth/**` -> `auth-service:8080`
  - `/api/v1/movies/**`, `/api/v1/genres/**`, `/api/v1/languages/**` -> `movie-service:8081`
  - `/api/v1/theatres/**`, `/api/v1/screens/**`, `/api/v1/seats/**`, `/api/v1/cities/**` -> `theatre-service:8082`
  - `/api/v1/shows/**` -> `show-service:8083`
  - `/api/v1/bookings/**` -> `booking-service:8084`
  - `/api/v1/payments/**` -> `payment-service:8085`
  - `/api/v1/notifications/**` -> `notification-service:8086`
- Strips spoofable internal headers (`x-user-id`, `x-user-roles`, `x-internal-secret`) and injects verified user identity from validated JWTs.

---

## 6. Current Movie / Show / Seat Flow

- **Movie Catalog**: Calls `GET /api/v1/movies` via Gateway. Normalizes DTO fields for display.
- **Show Selection**: Calls `GET /api/v1/shows/movie/{movieId}`. Renders real shows if available.
- **Seat Map (`renderSeatMap`)**:
  - Defect identified: Backend `ShowSeatResponse` returns `{ id, showId, seatId, price, status }` without a `seatNumber` field. The frontend matched `s.seatNumber === seatId`, which always evaluated to `undefined`.
  - As a result, `showSeatUuid` fell back to synthetic UUIDs (`00000000-0000-0000-0000-...`), breaking backend seat lock integration.
  - Hardcoded booked seats in `generateBookedSeats()` (`['A3', 'A4', ...]`) masked the missing mapping.

---

## 7. Current Booking Flow

- User selects seats -> clicks "Proceed" (`goToFood()`).
- Calls `POST /api/v1/bookings` with `userId`, `showId`, `showSeatIds`, and `paymentMethod`.
- **Critical Defect identified**: Lines 723–733 in `app.js` contained a fallback reservation mode:
  ```javascript
  // In offline/mock test environments, generate a resilient pending booking
  state.currentBooking = {
    id: window.VibeCheckApi.generateUUID(),
    bookingReference: 'BK' + Date.now().toString().slice(-10).toUpperCase(),
    totalAmount: state.selectedSeats.reduce((s, x) => s + x.price, 0),
    status: 'PENDING'
  };
  ```
  If the real booking API failed, the UI fabricated a fake booking and proceeded anyway.

---

## 8. Current Payment Flow

- Step 3 Payment (`processPayment()`):
  - Calls `POST /api/v1/payments` via Gateway.
  - **Critical Defect identified**: Lines 924–939 wrapped the real payment call and booking confirmation in individual `try/catch` blocks that swallowed errors:
    ```javascript
    try {
      const paymentRes = await window.VibeCheckApi.payments.initiatePayment(...);
      ...
    } catch (payErr) {
      console.warn('Remote payment service notice:', payErr.message);
    }
    ```
    Even if payment failed (or timed out, or threw 400/409/500), it proceeded to confirm and displayed a confirmed ticket!
- **Payment Idempotency Defect**: `processPayment` did not pass a stable `idempotencyKey`, causing each click/retry to generate a brand new UUID, violating idempotency guarantees.

---

## 9. Current Ticket / QR Flow

- Displays booking reference, movie title, theatre, time, seats, and total amount.
- QR code is rendered via HTML5 canvas in `drawQR(data)`.
- QR data currently uses local state fields; requires ensuring it only draws real confirmed booking data returned by the backend.

---

## 10. Current Logout Behavior

- `logout()` in `app.js` calls `window.VibeCheckApi.auth.logout(userId)`.
- Backend revokes refresh token in Redis/MySQL (`POST /api/v1/auth/logout/{userId}`).
- Client clears `localStorage` via `Storage.clearAuth()`.
- Resets user menu UI back to "Sign In" button.

---

## 11. Current Loading States

- Catalog loading has spinner overlay.
- "Sign In" button shows "Signing In..." and disables.
- "Proceed" button in seat selection shows spinner and disables.
- Payment shows full-screen `#payOverlay` ("Processing your payment...").

---

## 12. Current Error States

- Toast notifications display error messages.
- Needs hardening: when booking creation or payment fails, error must remain visible, overlay must dismiss, buttons must re-enable, and no fake success state should occur.

---

## 13. Dead / Non-Functional Buttons Identified

| Button / Element | Current Action | Required Fix |
|---|---|---|
| Mobile OTP "Get OTP" / "Verify OTP" | Hardcoded synthetic login | Provide clear message requiring email/password or wire to real auth |
| Social Login "Continue with Google" | Synthetic login without OAuth | Provide informative notice that social login is in enterprise federated tier |
| Hero "▶ Trailer" / Detail "▶ Watch Trailer" | `showToast('Trailer coming soon!')` | Informative toast (acceptable) or embed trailer modal |
| Header Nav: Events, Plays, Sports | Switches tab filter with placeholder events | Keep filter functional with real movies / events |

---

## 14. Mock / Simulated Behavior Reachable From Production UI

1. `renderDefaultShows(movie)`: Hardcoded showtimes if backend shows empty.
2. `generateBookedSeats()`: Hardcoded booked seats `['A3', 'A4', ...]`.
3. `goToFood()` fallback: Fabricating `BK...` booking on error.
4. `processPayment()` error swallowing: Swallowing payment failures and displaying fake tickets.
5. `verifyOTP()`: Hardcoded customer user object without backend token issuance.

---

## 15. Mismatch Between Frontend API Calls and Backend Contracts

1. **Seat Mapping**: `ShowSeatResponse` has `id` (ShowSeat UUID), `showId`, `seatId`, `price`, `status`, but no `seatNumber`. Frontend needs sequential mapping to real show seats when `seatNumber` is absent.
2. **Payment Idempotency**: Frontend failed to supply a persistent `idempotencyKey` per booking attempt, leading to duplicate payment requests on retries.
3. **Response Envelope**: `booking-service` returns `ApiResponse<BookingResponse>` where real data is in `.data`. Frontend must consistently unwrap `.data`.

---

## 16. Deployment-Specific Configuration Problem

- `web/nginx.conf` proxies `/api/` to `http://gateway-service:8079/api/`.
- In `web/js/config.js`, `determineApiBaseUrl()` defaulted to `http://localhost:8079` only if `window.location.port === '8079'`. When accessed via standard reverse proxy (port 80), it should use same-origin `window.location.origin`.
- `docker-compose.yml` was missing a `frontend` service definition for automated full-stack container runs.

---

## Summary of Action Items for Milestone 34 Hardening

1. **Eliminate All Fake Success Paths**:
   - Stop swallowing payment errors in `processPayment()`.
   - Stop fabricating fake bookings in `goToFood()`.
   - Prevent fake tickets when backend confirmation does not succeed.
2. **Correct Real Show Seat Mapping**:
   - Map real backend `ShowSeat` entities to the seat layout so real show seat UUIDs are sent to `booking-service`.
3. **Fix Payment Idempotency**:
   - Maintain a deterministic `paymentIdempotencyKey` attached to `state.currentBooking`.
4. **Harden Authentication**:
   - Guarantee only genuine backend JWT tokens and real user records authenticate the UI.
5. **Ensure Docker Compose & Verification Automation**:
   - Add frontend service or verify container builds.
   - Author `scripts/verify-milestone-34.ps1`.
