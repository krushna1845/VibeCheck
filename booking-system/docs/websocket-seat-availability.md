# WebSocket Real-Time Seat Availability — Technical Documentation

**Milestone 4 | booking-service**

---

## 1. Overview

The **booking-service** exposes a Spring WebSocket endpoint with STOMP messaging and SockJS fallback that broadcasts real-time seat availability updates to all subscribed clients. Any UI subscribed to a show topic receives instant push notifications whenever a seat changes state — no polling required.

---

## 2. Architecture

```mermaid
graph TD
    subgraph booking-service
        BC[BookingServiceImpl] -->|1. createBooking| SAP[SeatAvailabilityPublisher]
        BC -->|2. confirmBooking| SAP
        BC -->|3. cancelBooking| SAP
        BC -->|4. expireBooking| SAP
        SAP --> WSAW[WebSocketSeatAvailabilityPublisher]
        WSAW -->|convertAndSend| SMT[SimpMessagingTemplate]
        SMT -->|/topic/show/{showId}| BROKER[In-Memory STOMP Broker]
    end

    BROKER -->|push| C1[Browser Client 1]
    BROKER -->|push| C2[Browser Client 2]
    BROKER -->|push| CN[Mobile Client N]
```

---

## 3. Technology Stack

| Component | Technology |
|---|---|
| Protocol | WebSocket + STOMP |
| Fallback | SockJS (HTTP long-polling, EventSource) |
| Broker | Spring In-Memory STOMP Broker |
| Serialization | Jackson JSON |
| Dependency | `spring-boot-starter-websocket` |

---

## 4. WebSocket Endpoint

| Property | Value |
|---|---|
| Endpoint URL | `/ws-seat-availability` |
| SockJS URL | `/ws-seat-availability/info` |
| Application prefix | `/app` |
| Broker prefix | `/topic` |
| Show-specific topic | `/topic/show/{showId}` |

---

## 5. Event Types

All events share the same `SeatAvailabilityEvent` payload. The `eventType` field distinguishes them:

| `eventType` | Trigger | Seat Status Result |
|---|---|---|
| `SEAT_LOCKED` | `createBooking` — seat lock acquired | `LOCKED` (temporary) |
| `SEAT_RELEASED` | `cancelBooking` or `expireBooking` — lock released | `AVAILABLE` |
| `BOOKING_CONFIRMED` | `confirmBooking` — payment success | `BOOKED` (permanent) |
| `BOOKING_CANCELLED` | `cancelBooking` — user/admin cancels | `AVAILABLE` |
| `BOOKING_EXPIRED` | `expireBooking` — TTL elapsed | `AVAILABLE` |

> **Note:** `BOOKING_CANCELLED` and `BOOKING_EXPIRED` each emit **two** WebSocket messages — one for the booking lifecycle event and one `SEAT_RELEASED` event — ensuring clients can react to each semantic change independently.

---

## 6. Event Payload Schema

```json
{
  "eventId":         "3f0a2e11-fa60-41d1-bacd-000000000001",
  "eventType":       "SEAT_LOCKED",
  "showId":          "550e8400-e29b-41d4-a716-446655440000",
  "showSeatIds":     ["a1b2c3d4-...", "e5f6g7h8-..."],
  "bookingId":       "99f00000-0000-0000-0000-000000000001",
  "bookingReference":"BK1A2B3C4D5E",
  "userId":          "00000000-0000-0000-0000-000000000042",
  "timestamp":       "2026-08-08T18:00:00Z"
}
```

### Field Descriptions

| Field | Type | Description |
|---|---|---|
| `eventId` | `string (UUID)` | Unique identifier for this event instance |
| `eventType` | `SeatAvailabilityEventType` | Type of availability change |
| `showId` | `UUID` | The show this event belongs to |
| `showSeatIds` | `UUID[]` | Affected seat IDs |
| `bookingId` | `UUID` | Booking aggregate ID |
| `bookingReference` | `string` | 12-character human-readable reference (e.g. `BK1A2B3C4D5E`) |
| `userId` | `UUID` | User who triggered the action |
| `timestamp` | `Instant (ISO-8601)` | Server-side event creation time |

---

## 7. Event Trigger Map

```
createBooking()
    └─ lockSeats() [success]
        └─ publishes: SEAT_LOCKED

confirmBooking()
    └─ publishes: BOOKING_CONFIRMED

cancelBooking()
    └─ publishes: BOOKING_CANCELLED
    └─ publishes: SEAT_RELEASED

expireBooking()
    └─ publishes: BOOKING_EXPIRED
    └─ publishes: SEAT_RELEASED
```

---

## 8. Client Integration

### JavaScript (SockJS + STOMP.js)

```javascript
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const SHOW_ID = "550e8400-e29b-41d4-a716-446655440000";

const stompClient = new Client({
  webSocketFactory: () => new SockJS("http://localhost:8083/ws-seat-availability"),
  reconnectDelay: 5000,
  onConnect: () => {
    console.log("Connected to WebSocket broker");

    stompClient.subscribe(`/topic/show/${SHOW_ID}`, (message) => {
      const event = JSON.parse(message.body);
      console.log(`[${event.eventType}] seats:`, event.showSeatIds);

      switch (event.eventType) {
        case "SEAT_LOCKED":
          markSeatsLocked(event.showSeatIds);
          break;
        case "SEAT_RELEASED":
          markSeatsAvailable(event.showSeatIds);
          break;
        case "BOOKING_CONFIRMED":
          markSeatsBooked(event.showSeatIds);
          break;
        case "BOOKING_CANCELLED":
        case "BOOKING_EXPIRED":
          markSeatsAvailable(event.showSeatIds);
          break;
      }
    });
  },
  onDisconnect: () => console.log("Disconnected"),
});

stompClient.activate();
```

### Dependency (npm)

```bash
npm install @stomp/stompjs sockjs-client
```

---

## 9. Implementation Components

| Component | File | Role |
|---|---|---|
| `SeatAvailabilityEventType` | `event/SeatAvailabilityEventType.java` | Enum of all 5 event types |
| `SeatAvailabilityEvent` | `event/SeatAvailabilityEvent.java` | Immutable event record / payload |
| `SeatAvailabilityPublisher` | `event/SeatAvailabilityPublisher.java` | Domain interface |
| `WebSocketSeatAvailabilityPublisher` | `event/WebSocketSeatAvailabilityPublisher.java` | STOMP broadcast implementation |
| `WebSocketConfig` | `config/WebSocketConfig.java` | STOMP + SockJS endpoint registration |
| `SecurityConfig` | `config/SecurityConfig.java` | Permit `/ws-seat-availability/**` paths |
| `BookingServiceImpl` | `service/impl/BookingServiceImpl.java` | Triggers all 5 event types |

---

## 10. Tests

| Test Class | Type | Coverage |
|---|---|---|
| `WebSocketSeatAvailabilityPublisherTest` | Unit | All 5 event broadcasts, topic path, error resilience, null safety |
| `BookingServiceImplTest` | Unit | SEAT_LOCKED on create; BOOKING_CONFIRMED on confirm; BOOKING_CANCELLED + SEAT_RELEASED on cancel; BOOKING_EXPIRED + SEAT_RELEASED on expire; no events on idempotent/failed operations |

Run tests:
```bash
mvn test -pl booking-service
```

---

## 11. STOMP Topic Design Decision

The topic `/topic/show/{showId}` was chosen over a global `/topic/seats` for the following reasons:

1. **Targeted subscriptions** — clients only receive updates for the show they are actively viewing.
2. **Scalability** — STOMP filtering happens at subscription time; no client-side filtering needed.
3. **Reduced message volume** — events are scoped and don't fan-out unnecessarily to all connected clients.

---

## 12. Error Handling

- **Broker unavailable**: `WebSocketSeatAvailabilityPublisher` catches all exceptions from `SimpMessagingTemplate` and logs them at `ERROR` level. The exception is **not** propagated, so a WebSocket failure will **never** cause a booking transaction to roll back.
- **Null events**: Events with `null` body or `null showId` are silently dropped with a `WARN` log.
- **SockJS fallback**: If the client cannot establish a WebSocket connection, SockJS transparently falls back to HTTP streaming or polling.
