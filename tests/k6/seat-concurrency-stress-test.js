import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
export const successfulBookings = new Counter('successful_bookings');
export const seatConflicts = new Counter('seat_conflicts_rejected');
export const unexpectedErrors = new Counter('unexpected_errors');
export const bookingLatency = new Trend('booking_latency_ms');

export const options = {
    scenarios: {
        // 1. Browsing & Catalog Traffic
        browsing_traffic: {
            executor: 'ramping-vus',
            startVUs: 10,
            stages: [
                { duration: '30s', target: 50 },
                { duration: '1m', target: 100 },
                { duration: '30s', target: 0 },
            ],
            exec: 'browseCatalog',
        },
        // 2. High-Concurrency Seat Lock Collision (Flash Sale / Popular Show Race)
        hot_seat_race: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: 500,
            maxDuration: '2m',
            exec: 'competeForHotSeats',
            startTime: '15s',
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<600'], // 95% of requests must complete below 600ms
        'unexpected_errors': ['count==0'],  // Zero unhandled 5xx server errors
        'successful_bookings': ['count<=5'], // Exactly the fixed number of seats available can be booked
    },
};

const BASE_URL = __ENV.GATEWAY_URL || 'http://127.0.0.1:8079';
const TARGET_SHOW_ID = __ENV.SHOW_ID || '11111111-2222-3333-4444-555555555555';
const TARGET_SEAT_ID = __ENV.SEAT_ID || 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';

export function setup() {
    console.log(`Setting up stress test against Gateway: ${BASE_URL}`);
    // Register and login an admin / seed user
    const email = `k6_seed_${Date.now()}@vibecheck.com`;
    const password = 'Password@123';

    const regPayload = JSON.stringify({
        email: email,
        password: password,
        firstName: 'Load',
        lastName: 'Tester',
        phoneNumber: '+15559990001',
        roles: ['ROLE_CUSTOMER'],
    });

    const regRes = http.post(`${BASE_URL}/api/v1/auth/register`, regPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    const loginPayload = JSON.stringify({ email, password });
    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    const token = loginRes.json('accessToken') || loginRes.json('token');
    return { token };
}

export function browseCatalog(data) {
    const headers = data.token ? { 'Authorization': `Bearer ${data.token}` } : {};

    // 1. Movies list
    const moviesRes = http.get(`${BASE_URL}/api/v1/movies`, { headers });
    check(moviesRes, { 'movies status is 200': (r) => r.status === 200 });

    // 2. Theatres list
    const theatresRes = http.get(`${BASE_URL}/api/v1/theatres`, { headers });
    check(theatresRes, { 'theatres status is 200': (r) => r.status === 200 });

    sleep(1);
}

export function competeForHotSeats(data) {
    const userId = `vu-user-${__VU}-${__ITER}`;
    const bookingPayload = JSON.stringify({
        userId: '00000000-0000-0000-0000-' + String(__VU).padStart(12, '0'),
        showId: TARGET_SHOW_ID,
        showSeatIds: [TARGET_SEAT_ID],
        idempotencyKey: `k6-idem-${__VU}-${__ITER}`,
    });

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`,
    };

    const start = Date.now();
    const res = http.post(`${BASE_URL}/api/v1/bookings`, bookingPayload, { headers });
    bookingLatency.add(Date.now() - start);

    if (res.status === 201) {
        successfulBookings.add(1);
        console.log(`[VU ${__VU}] WON HOT SEAT LOCK! Status 201 Created.`);
    } else if (res.status === 400 || res.status === 409 || res.status === 422) {
        // Seat already locked or booked by another concurrent customer
        seatConflicts.add(1);
    } else if (res.status === 429) {
        // Gateway rate limiting active
        console.log(`[VU ${__VU}] Throttled by Gateway (429 Too Many Requests)`);
    } else {
        unexpectedErrors.add(1);
        console.error(`[VU ${__VU}] Unexpected response status ${res.status}: ${res.body}`);
    }
}
