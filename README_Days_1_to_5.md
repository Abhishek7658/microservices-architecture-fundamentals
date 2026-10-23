# Distributed API Architecture, Resilience & Observability

**Project:** `microservices-architecture-fundamentals`  
**Work period:** 19–23 October 2026  
**Author:** Abhishek

## Overview

This README consolidates the Day 1–5 work notes and the behavior observed during manual verification. It covers service boundaries, API contracts, Gateway security, resilience, idempotency, distributed tracing, correlation IDs, and failure testing.

> **Evidence note:** The HTTP results below are based on the Postman responses and terminal/Jaeger observations captured during the work. Manual Postman checks are not a substitute for a separately verified automated integration-test suite.

## Architecture and local services

| Component | Local address / port | Role |
|---|---|---|
| API Gateway | `http://localhost:8080` | Entry point, routing, security and rate limiting |
| Product Service | `http://localhost:8081` | Product API |
| Order Service | `http://localhost:8082` | Order API and order workflow |
| User Service | `http://localhost:8090` | User lookup used by Order Service |
| Kafka | `localhost:9092` | Asynchronous event transport |
| Redis | `localhost:6379` | Idempotency storage and supporting state |
| Jaeger UI | `http://localhost:16686` | Distributed trace inspection |
| OpenTelemetry Collector | `localhost:4317`, `localhost:4318` | Telemetry ingestion |

Other services include Payment, Inventory, and Notification. The synchronous order request does not necessarily involve every registered service; asynchronous event processing may appear in separate traces.

## Starting local infrastructure

Run from the repository root in PowerShell.

```powershell
docker compose -f .\infrastructure\Kafka\docker-compose.yml up -d
docker compose -f .\observability\docker-compose.yml up -d
docker ps
```

Redis was run locally in Docker during verification. Check for an existing Redis container before creating another container with the same name or publishing the same host port.

Start Spring Boot services using the project's existing run configurations or Maven commands. Wait for each service's startup-complete message before testing APIs.

## Day 1 — Service boundaries and API contracts

### Work completed

- Documented service boundaries in `docs/service-boundaries.md`.
- Documented the Order API contract in `docs/contracts/order-api.yaml`.
- Documented Kafka events in `docs/kafka-events.md`.
- Added a trust-boundary diagram in `docs/architecture-trust-boundaries.mmd`.
- Updated Order Service error handling so an invalid order quantity is rejected as a client error.

### Observed behavior

- A valid `POST /api/orders` request returned `201 Created`.
- An order lookup returned `200 OK` with a confirmed order in the captured test.
- An invalid quantity was rejected with a client error.

## Day 2 — Gateway security and request protection

The Gateway work focused on routing, authentication and authorization, request-size protection, rate limiting, and safe downstream error handling.

### Routing context

- Gateway: port `8080`
- User Service: port `8090`
- Order Service: port `8082`; Gateway rewrites `/api/orders/**` to `/api/v1/orders/**`
- Product Service: port `8081`

### Observed behavior

- A Product API request without a token returned `401 Unauthorized`.
- A request with a valid User JWT returned `200 OK`.
- An expired token was rejected with `401 Unauthorized`.
- A User token attempting an Admin-only operation returned `403 Forbidden`.
- An Admin-authorized create request returned `201 Created`.
- An oversized request returned `413 Payload Too Large`.
- When a downstream service was unavailable, the Gateway returned a safe `503 Service Unavailable`; the request succeeded after the service restarted.

Rate-limit headers were observed, including `X-RateLimit-Limit` and `X-RateLimit-Remaining`. A later Day 5 Postman run explicitly observed `429 Too Many Requests`.

## Day 3 — Order resilience and idempotency

The Order Service's development profile (`dev`) was used. The recorded User Service client settings included a 2,000 ms connect/read timeout, up to three retry attempts, and a 500 ms initial backoff. The circuit breaker was configured with a 50% failure threshold, a minimum of five calls, a sliding window of ten calls, a 10-second open-state wait, and three permitted half-open calls. `UserNotFoundException` was configured to be ignored by the circuit breaker.

The idempotency mechanism used Redis keys prefixed by `idempotency:order:` and a 24-hour TTL. A SHA-256 fingerprint was calculated from `userId`, `productId`, `productName`, `quantity`, and `amount`.

### Idempotency behavior

- Repeating the same key and same payload returns the cached response for the original order.
- Reusing the same key with a different payload returns `409 Conflict`.
- Successful response data is stored after the database transaction commits.
- A reservation is deleted on transaction rollback.

**Known limitation:** If the database commit succeeds but the subsequent Redis write fails, the idempotency reservation may remain in a processing state until its TTL expires. A database-backed idempotency record would provide stronger transactional guarantees.

### Verification recorded

- Maven `clean compile`, `test-compile`, and `test` completed successfully during Day 3.
- Repeating a request with the same idempotency key and payload returned the same order.
- Reusing that key with a changed payload returned `409 Conflict`.
- With User Service unavailable, the Order API returned a controlled `503`; requests succeeded after recovery.
- Circuit-breaker state changes from `CLOSED` to `OPEN`, `HALF_OPEN`, and back to `CLOSED` were observed through the available circuit-breaker endpoint.
- The Day 3 changes were committed and pushed as `cdb4c22` (`day 3 21 th october`).

## Day 4 — Distributed tracing and correlation IDs

### Successful request evidence

A request sent through `POST http://localhost:8080/api/orders` returned `201 Created` for order `16453`. The response included correlation ID:

`c7f876c8-bd16-46f1-9e75-827fb25ebb2a`

Captured terminal logs showed:

- Gateway: request status `201`, response duration approximately `2620 ms`.
- Order Service: `POST /api/v1/orders`, status `201`, duration approximately `2251 ms`.
- User Service: `GET /api/users/1`, status `200`, duration approximately `1095 ms`.
- Payment, Inventory, and Notification logs included related downstream event activity and the same correlation ID.

### Jaeger observations

The successful synchronous HTTP trace contained 21 spans across three services: `api-gateway`, `order-service`, and `user-service`. The root operation was `api-gateway: POST /api/orders/**`.

Separate asynchronous outbox/event-processing traces were also observed. The total number of services registered in Jaeger should not be interpreted as the number participating in one request trace.

The correlation ID and Jaeger trace ID are different identifiers: the correlation ID helps locate related log entries, while the trace ID identifies a distributed trace.

The Gateway URLs `/actuator` and `/actuator/metrics/http.server.requests` returned `404` during a check. Metrics were optional for that day's verification and were not claimed as verified through those endpoints.

## Day 5 — Resilience capstone and failure testing

### Test evidence

| Scenario | Observed result | Response time | Result |
|---|---|---:|---|
| Normal order creation | `201 Created`, order `16454` | `7.05 s` | Successful |
| Invalid quantity (`0`) | `400 Bad Request`; `quantity: must be greater than or equal to 1` | `179 ms` | Validation confirmed |
| User Service unavailable | `503 Service Unavailable`; `User Service is currently unavailable` | `2.09 s` | Controlled failure |
| User Service restored | `201 Created`, order `16455` | `1.16 s` | Recovery confirmed |
| Rate limiting | `429 Too Many Requests` observed in a 101-iteration Postman run | Run duration `16.697 s` | Rate limit reached |

The `503` response included a trace ID, allowing the failed request to be investigated. The observed outage response time was consistent with the configured 2-second User Service timeout, but the screenshot alone does not establish the exact number of retries performed.

### Postman assertion note

The initial rate-limit assertion allowed only `200` and `429`. Successful order creation returned `201`, so those iterations failed the assertion even though the API response was valid for the POST endpoint. A general assertion can allow `201` and `429`, but a dedicated rate-limit test should explicitly assert `429` after the limit is exceeded.

### Architectural trade-offs

- **Timeouts:** Bound the wait for an unavailable dependency, but can cause requests to fail when the dependency is slow.
- **Retries:** Help with transient errors, but increase latency and downstream load; retries should be bounded and should not blindly repeat client errors.
- **Circuit breaker:** Helps prevent repeated calls to an unhealthy dependency. State transitions should be claimed only when logs or actuator evidence confirm them.
- **Safe error responses:** Give clients a controlled failure response without exposing internal stack traces.
- **Rate limiting:** Protects the Gateway and downstream services from excessive traffic; the exact key and time window should be documented from the actual configuration.
- **Idempotency:** Prevents duplicate order creation on repeated requests with the same key and payload, with the Redis/database consistency limitation described above.

## Running checks

Use the Maven Wrapper from the correct repository/module directory:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test-compile
.\mvnw.cmd test
```

The recorded Day 3 successful test run does not prove tests were rerun after Day 5.

## Known limitations and follow-up

- Day 5 response times are individual observations, not load-test benchmarks.
- Manual Postman scenarios do not replace a separately verified automated integration-test suite.
- The exact rate-limit key and time window should be confirmed from Gateway configuration.
- Retry counts during the Day 5 outage should be confirmed from logs if required.
- Exact Day 4 negative-case request/response details were not included in the available notes; Day 5 invalid-quantity evidence is recorded above.
- The Actuator metrics URLs tested on Day 4 returned `404`; optional metrics were not claimed as verified.
- The Redis idempotency flow has a database-commit/Redis-write consistency edge case.

## Git and delivery notes

Day 3's recorded commit is `cdb4c22`. Day 4 was documented as a verification-only task with no application code changes. Before committing Day 5 documentation or other changes, inspect the actual working tree:

```powershell
git status
git diff --stat
git diff
git log -1 --oneline
```

Commit only intentionally changed and reviewed files. Confirm remote synchronization before reporting the repository as up to date.

## Summary

Across Days 1–5, the work covered service boundaries and contracts, Gateway security and protection, Order Service resilience and idempotency, distributed tracing and correlation IDs, and manual resilience/failure verification. The captured evidence shows successful order creation, invalid-input rejection, controlled failure during a User Service outage, recovery after restart, and a rate-limit response. Automated integration-test coverage and the final repository state should be checked separately before declaring every delivery criterion complete.
