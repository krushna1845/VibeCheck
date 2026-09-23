"""
VibeCheck Movie Booking Platform - Locust Load & Concurrency Test
Simulates high-concurrency seat acquisition, catalog queries, and booking race conditions.
"""

import time
import uuid
from locust import HttpUser, task, between, events

class MoviePlatformUser(HttpUser):
    wait_time = between(0.1, 1.0)
    token = None
    user_id = None

    def on_start(self):
        """Register and authenticate user for session."""
        timestamp = int(time.time() * 1000)
        self.user_id = str(uuid.uuid4())
        email = f"locust_{timestamp}_{uuid.uuid4().hex[:6]}@vibecheck.com"
        password = "Password@123"

        reg_data = {
            "email": email,
            "password": password,
            "firstName": "Locust",
            "lastName": "User",
            "phoneNumber": f"+1555{timestamp % 10000000:07d}",
            "roles": ["ROLE_CUSTOMER"]
        }

        # 1. Register
        with self.client.post("/api/v1/auth/register", json=reg_data, catch_response=True) as response:
            if response.status_code in (200, 201):
                response.success()
            else:
                response.failure(f"Registration failed: {response.text}")
                return

        # 2. Login
        login_data = {"email": email, "password": password}
        with self.client.post("/api/v1/auth/login", json=login_data, catch_response=True) as response:
            if response.status_code == 200:
                body = response.json()
                self.token = body.get("accessToken") or body.get("token")
                if not self.token and "data" in body:
                    self.token = body["data"].get("accessToken")
                response.success()
            else:
                response.failure(f"Login failed: {response.text}")

    @task(3)
    def browse_movies(self):
        """Browse movie catalog."""
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        self.client.get("/api/v1/movies", headers=headers, name="/api/v1/movies")

    @task(2)
    def browse_theatres(self):
        """Browse theatres."""
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        self.client.get("/api/v1/theatres", headers=headers, name="/api/v1/theatres")

    @task(1)
    def race_for_hot_seats(self):
        """Concurrent lock acquisition for hot seat."""
        if not self.token:
            return

        headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }

        hot_show_id = "11111111-2222-3333-4444-555555555555"
        hot_seat_id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"

        payload = {
            "userId": self.user_id,
            "showId": hot_show_id,
            "showSeatIds": [hot_seat_id],
            "idempotencyKey": f"locust-idem-{uuid.uuid4()}"
        }

        with self.client.post("/api/v1/bookings", json=payload, headers=headers, catch_response=True) as resp:
            if resp.status_code == 201:
                resp.success() # Acquired lock & booked!
            elif resp.status_code in (400, 409, 422):
                # Seat unavailable or already locked — expected concurrency conflict
                resp.success()
            elif resp.status_code == 429:
                # Gateway rate limiter
                resp.success()
            else:
                resp.failure(f"Unexpected status: {resp.status_code} - {resp.text}")
