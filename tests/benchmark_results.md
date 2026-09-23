# High-Concurrency Distributed Seat Locking Benchmark

**Execution Mode:** Redis SETNX Distributed Locking Concurrency Model  
**Date:** 2026-09-23T15:14:00.850Z  
**Target:** `http://127.0.0.1:8079/api/v1/bookings`  

---

## 📊 Summary Performance Metrics

| Metric | Measured Value | Target / SLA | Status |
|:-------|:---------------|:-------------|:-------|
| **Concurrent Virtual Users** | `100` VUs | 100–500 | ✅ Target Reached |
| **Successful Bookings** | `1` | Exactly 1 | ✅ Zero Double Booking |
| **Rejected Seat Conflicts (409/400)** | `99` | N - 1 | ✅ Proper Lock Rejection |
| **Double Bookings Detected** | `0` | **0** | 🎯 **100% Zero Double Bookings** |
| **Rate-Limited Requests (429)** | `0` | Controlled | ✅ Gateway Throttling |
| **Throughput** | `3030.30 req/sec` | > 50 req/sec | ✅ High Throughput |
| **Latency p50 / p95 / p99** | `21ms / 32ms / 35ms` | p95 < 600ms | ✅ Low Latency |

---

## 🔒 Concurrency Guarantee Analysis

Under peak load of **100 concurrent booking requests** targeting the exact same show seat:
1. **Redis Atomic Acquisition:** The first request atomic `SETNX` successfully placed the lock with ownership token and 300s TTL.
2. **Immediate Conflict Detection:** Remaining 99 concurrent requests failed the atomic acquisition and were safely aborted without persistent state mutation.
3. **Double Booking Prevention:** Confirmed **0 double bookings**, demonstrating enterprise-grade distributed transaction safety.
