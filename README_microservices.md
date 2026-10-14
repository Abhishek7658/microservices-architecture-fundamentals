# Microservices Architecture Fundamentals

## Overview

This repository demonstrates a Spring Boot microservices architecture built around independently running business services, an API Gateway, asynchronous messaging, distributed workflow coordination, and shared infrastructure.

The main focus is the microservices system and how its components work together:

- **API Gateway** — central entry point for routing, request policies, and cross-cutting concerns.
- **User, Product, Order, Payment, Inventory, and Notification services** — separate applications organized around business capabilities.
- **Apache Kafka** — messaging infrastructure for asynchronous communication and event-driven workflows.
- **Saga pattern** — coordination of multi-service business operations using a sequence of local transactions and compensating actions where applicable.
- **Redis** — infrastructure used by the API Gateway rate limiter to maintain request counters.
- **Observability stack** — project infrastructure for tracing and monitoring service requests.

The repository also contains a `monolith/monolithic-app` example for comparison. It is a secondary learning example; the primary architecture described below is the microservices system.

---

## Architecture at a glance

```text
                         Clients / Postman
                                |
                                v
                    +------------------------+
                    |      API Gateway       |
                    | Routing / request      |
                    | policies / rate limit   |
                    +------------------------+
                       |       |       |
             -----------       |       -----------
             |                 |                 |
             v                 v                 v
      +-------------+   +-------------+   +-------------+
      | User Service|   |Product Svc  |   | Order Service|
      +-------------+   +-------------+   +-------------+
                               ^                 |
                               |                 | business workflow
                               |                 v
                         +-------------+   +-------------+
                         | Inventory   |<->| Payment     |
                         | Service     |   | Service     |
                         +-------------+   +-------------+
                                \             /
                                 \           /
                                  v         v
                              +------------------+
                              | Notification Svc |
                              +------------------+

                   Services / event producers and consumers
                                  |
                                  v
                           +--------------+
                           | Apache Kafka |
                           +--------------+

                           +--------------+
                           | Redis        |
                           | Gateway rate |
                           | limit state  |
                           +--------------+

             Observability infrastructure supports telemetry
             and troubleshooting across the applications.
```

> This is a conceptual view of the repository's components, not a guarantee that every arrow represents a currently configured runtime call. Confirm exact event topics, producers/consumers, and Saga orchestration/choreography from the corresponding source code and configuration before treating them as an implementation contract.

## Repository structure

```text
microservices-architecture-fundamentals/
├── microservices/
│   ├── api-gateway/
│   ├── user-service/
│   ├── product-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── inventory-service/
│   └── notification-service/
├── infrastructure/
│   └── Kafka / supporting infrastructure configuration
├── observability/
│   └── telemetry and observability configuration
├── monolith/
│   └── monolithic-app/
└── README.md
```

Directory contents may evolve. Use each module's `pom.xml`, configuration files, and application entry point as the source of truth for its dependencies and runtime settings.

---

## Microservices and responsibilities

| Component | Main responsibility |
|---|---|
| **API Gateway** | Routes client requests to the appropriate backend service and applies cross-cutting request policies. |
| **User Service** | User-related operations and user information. |
| **Product Service** | Product catalog and product information. |
| **Order Service** | Order creation and order lifecycle operations. |
| **Payment Service** | Payment-related operations within the order workflow. |
| **Inventory Service** | Inventory and stock-related operations. |
| **Notification Service** | Sends notifications associated with business events. |
| **Apache Kafka** | Provides messaging infrastructure for asynchronous, event-driven communication. |
| **Redis** | Stores rate-limit counters used by the API Gateway implementation. |
| **Observability infrastructure** | Supports telemetry collection and investigation of cross-service behavior. |

Each service is a separate Spring Boot application. This separation supports independent development and deployment, while introducing distributed-system concerns such as network failures, eventual consistency, event delivery, and cross-service troubleshooting.

## API Gateway

The API Gateway runs on port **8080** in the current local setup. The configured routes include:

| Gateway path | Backend service | Backend port | Route behavior |
|---|---|---:|---|
| `/api/users/**` | User Service | `8090` | Forwarded to the User Service. |
| `/api/orders/**` | Order Service | `8082` | Rewrites the gateway path to the `/api/v1/orders` backend path. |
| `/api/products/**` | Product Service | `8081` | Rewrites the gateway path to the `/api/v1/products` backend path in the tested configuration. |
| `/api/payments/**` | Payment Service | `8084` | Rewrites the gateway path to `/payments` on the backend. |

The following ports are also present in the local setup:

| Component | Port |
|---|---:|
| Inventory Service | `8085` |
| Notification Service | `8083` |
| Redis | `6379` |

Check `microservices/api-gateway/src/main/resources/application.properties` for the active route definitions. A configured route only proves that the gateway has a route rule; it does not by itself prove that the target endpoint is healthy or that every HTTP method is authorized.

### Gateway request policies

The gateway includes a `RateLimitFilter` backed by `StringRedisTemplate`. The current policy uses a limit of **100 requests per 60-second window** and returns HTTP `429` when the observed counter exceeds the limit.

Key behavior from the implementation:

```java
private static final int MAX_REQUESTS = 100;
private static final int WINDOW_SECONDS = 60;

Long requestCount = redisTemplate.opsForValue().increment(key);

if (requestCount != null && requestCount == 1) {
    redisTemplate.expire(
        key, WINDOW_SECONDS, java.util.concurrent.TimeUnit.SECONDS
    );
}
```

The filter returns `X-RateLimit-Limit` and `X-RateLimit-Remaining` response headers. In the tested version, the counter key is based on the caller's remote IP address:

```java
String userId = request.getRemoteAddr();
String key = "rate_limit:user:" + userId;
```

This avoids trusting a caller-supplied `X-User-ID` header as an identity. In deployments behind a reverse proxy, configure trusted proxy handling before relying on client IP addresses; do not blindly trust arbitrary forwarded headers. The counter increment and expiry sequence should also be reviewed for atomicity under concurrent traffic before production use.

### Authentication and authorization

The Product Service has JWT-based authentication and route/method authorization rules. In the current local tests:

- The public product endpoint `GET /api/products/public` through the gateway returned **200 OK** after the product route rewrite was changed to the backend's `/api/v1/products` path.
- A protected product request without a token returned **401 Unauthorized**.
- A protected request with an invalid token returned **401 Unauthorized**.
- A valid Product Service access token was observed working in the service-side JWT filter logs for a protected request to `/api/v1/products/1`.

These observations do not establish that the gateway independently validates JWTs. Confirm the gateway's own authentication implementation and test the same token through both direct-service and gateway URLs before documenting gateway-level JWT validation as complete.

### Gateway logging and correlation

The gateway contains a `GatewayLoggingFilter` and the observed response included an `X-Correlation-Id` header. Correlation IDs help associate a client request with gateway logs and downstream processing. Logs should not include raw bearer tokens, secrets, or sensitive request data.

---


## Order Service resilience, idempotency, and transactional outbox

The Order Service includes resilience controls for calls to the User Service, Redis-backed idempotency for order creation, and a transactional outbox for publishing order-created events to Kafka.

### User Service resilience

- The Order Service calls the User Service through `UserServiceClient`.
- Retry behavior uses configured retry attempts and backoff for retryable connection/server failures.
- The User Service client is protected by a Resilience4j circuit breaker.
- When the User Service remains unavailable, the Order Service uses its fallback path and returns HTTP `503 Service Unavailable`.
- A missing user is treated as a `404 Not Found` rather than as a retryable availability failure.

These behaviors were manually exercised during local development. Configuration values should be checked in the active `application*.yml` files before being changed.

### Order creation idempotency

Order creation requires an `Idempotency-Key` header. The Order Service uses Redis keys with the prefix `idempotency:order:` and a 24-hour TTL.

- The first request claims the key with a `PROCESSING` state.
- A concurrent request that sees `PROCESSING` is rejected rather than creating another order at the same time.
- A successful request stores the serialized order response in Redis.
- A replay using the same key returns the stored response.

**Known limitation:** Redis and the relational database do not share one atomic transaction. If the database transaction fails after a response is written to Redis, the cached response can become stale. A process crash between database commit and Redis caching can also leave a committed order without a cached response. These edge cases are not covered by the current verification and may require a more robust idempotency-state/recovery design before high-volume production use.

### Transactional outbox

Order creation persists the `Order`, `OrderItem`, and an `OutboxEvent` within the database transaction. The outbox event uses event type `ORDER_CREATED` and begins in `PENDING` status.

A scheduled `OutboxEventProcessor` polls pending records, deserializes the event payload, publishes it to the Kafka topic `order-created`, waits for the send result, and marks the outbox record `PUBLISHED` after successful publication acknowledgement.

This design avoids the dual-write problem of committing an order to the database while attempting to publish the event directly in the same request path. It does **not** provide exactly-once delivery: a crash after Kafka accepts an event but before its outbox status is updated can result in a duplicate publication. Consumers should therefore handle duplicate events idempotently. The current processor also needs a database-backed claim/locking strategy if multiple Order Service instances may process the same outbox table concurrently.

### Task 03 verification evidence

The following checks were observed during local development:

| Check | Observed result | What it demonstrates |
|---|---|---|
| Order creation through API Gateway | `POST http://localhost:8080/api/orders` returned `201 Created`; order ID `16432` | The request reached the Order Service and created an order in the local test. |
| Outbox persistence and publication status | The `ORDER_CREATED` outbox row for aggregate ID `16432` reached `PUBLISHED` with a non-null `published_at` | The outbox record was persisted and the processor recorded successful Kafka publication acknowledgement. |
| Build | `.\mvnw.cmd clean compile` returned `BUILD SUCCESS` | The Order Service compiled at that point. |
| Idempotency replay | A same-key replay had previously returned the existing order ID during local testing | The tested replay path returned the stored response rather than creating another order. |
| Resilience behavior | Retry attempts/backoff and HTTP `503` fallback were observed during a User Service outage; successful recovery was also observed | The tested failure and recovery paths behaved as expected in that local run. |

These are manual local observations, not a substitute for repeatable automated integration tests. The outbox `PUBLISHED` status indicates that the producer send was acknowledged; it does not independently prove that a downstream Kafka consumer processed the event.

### Operational checks

For this task, the main local components used were API Gateway (`8080`), Order Service (`8082`), User Service (`8090`), Redis (`6379`), MySQL, and Kafka. Confirm active ports and connection settings in the current configuration rather than assuming they are identical in every environment.

Useful database verification query:

```sql
SELECT id, aggregate_id, event_type, status, created_at, published_at
FROM outbox_events
WHERE aggregate_id = '16432';
```

For a new test, use a fresh idempotency key, then verify the response, outbox row, and same-key replay. Avoid creating unnecessary test orders in a shared or persistent development database.

## Saga pattern and distributed workflows

The Saga pattern addresses business operations that span multiple services without relying on one distributed database transaction. A Saga is composed of local transactions. If a later step fails, the workflow may invoke compensating actions for earlier completed steps.

For an order workflow, the services that may participate include Order, Inventory, Payment, and Notification. The exact step order, event names, retry policy, and compensation logic must be read from the current service implementation and Kafka configuration; this README intentionally does not invent those details.

When documenting or extending a Saga, record:

1. The event or command that starts the workflow.
2. Each local transaction and the service responsible for it.
3. The success/failure event produced by each step.
4. The compensation action for each reversible step.
5. Retry, timeout, duplicate-message, and idempotency behavior.
6. How the final order status is persisted and observed.

A Saga is not the same as a single ACID transaction. Intermediate states can be visible, so service APIs and consumers should handle eventual consistency and duplicate delivery safely.

## Apache Kafka

Kafka provides the messaging layer for event-driven service communication. The repository includes an `infrastructure` directory for supporting configuration. Consult the actual Kafka configuration and service code to identify the broker address, topic names, serializers, producers, consumer groups, and event schemas.

Recommended operational checks:

- Ensure Kafka is running before starting services that require it.
- Verify that producer and consumer topic names match.
- Inspect application logs for connection, serialization, and consumer errors.
- Design consumers to tolerate duplicate delivery and make side effects idempotent.
- Define retry and dead-letter handling where required by the workflow.
- Avoid treating a successful publish as proof that downstream business processing completed.

## Redis

Redis runs on port **6379** in the current local setup and is used by the API Gateway rate limiter to track request counts.

The current filter uses a key prefix of `rate_limit:user:` and a 60-second expiry. For production, consider whether the limit should be per IP, authenticated principal, API key, or route; use trusted client-IP resolution when behind proxies; handle Redis failures deliberately; and consider an atomic rate-limiting algorithm if stricter window semantics are required.

## Observability

The repository includes an `observability` directory with telemetry-related configuration. Use the actual configuration files to determine which collectors, exporters, tracing backends, and metrics are enabled.

For distributed troubleshooting, correlate:

- the client request and gateway correlation ID;
- gateway route and response status;
- downstream service logs;
- Kafka producer/consumer processing;
- Saga state transitions and compensations;
- Redis or other infrastructure errors.

Never log bearer tokens, passwords, signing secrets, payment credentials, or unnecessary personal data.

---

## Local development and ports

The following ports were used in the current local project setup:

| Application / dependency | Port |
|---|---:|
| API Gateway | `8080` |
| Product Service | `8081` |
| Order Service | `8082` |
| Notification Service | `8083` |
| Payment Service | `8084` |
| Inventory Service | `8085` |
| User Service | `8090` |
| Redis | `6379` |

Kafka and observability ports should be taken from the current infrastructure configuration rather than assumed here.

### Suggested startup sequence

1. Start the infrastructure dependencies required by the services (for example, Kafka and Redis) using the repository's infrastructure configuration.
2. Start the business services required for the workflow.
3. Start the API Gateway.
4. Confirm application startup logs and verify each service's configured port.
5. Use Postman to test a public endpoint, a protected endpoint, invalid credentials, and the rate-limit behavior.

Run each Spring Boot module using its Maven wrapper from that module's directory. On Windows, the wrapper is typically `mvnw.cmd`; on Unix-like systems, use `./mvnw`. Review each module's `application.properties` and environment variables before starting it.

---

## Verification evidence

The following results were manually observed in Postman during the local Task 02 work:

| Test | Request | Observed result | Interpretation |
|---|---|---|---|
| Public product route | `GET http://localhost:8080/api/products/public`, no authentication | `200 OK` after correcting the route rewrite | Gateway routing to the public Product Service endpoint worked in this local test. |
| Protected product route without token | `GET http://localhost:8080/api/products`, no authentication | `401 Unauthorized` | The protected request was rejected. |
| Protected route with invalid token | Same protected gateway path with an invalid bearer token | `401 Unauthorized` | Invalid credentials were rejected. |
| Rate-limit headers | `GET http://localhost:8080/api/products/1` with test requests | `X-RateLimit-Limit: 100` and a decreasing `X-RateLimit-Remaining` value | Rate-limit headers and request counting were observed. |
| Rate-limit threshold | Collection Runner executed 101 iterations; the final visible request returned `429` | `429 Too Many Requests` on the final request | The configured limit was reached in that run. |
| Valid Product Service token in service logs | Protected backend request to `/api/v1/products/1` | Service logs showed a valid JWT and HTTP `200` | Backend token validation worked for that observed request; this alone does not prove independent gateway JWT validation. |
| Build | Gateway project build | Build success reported during task work | The project compiled at that point; this is not a substitute for automated tests. |

The Collection Runner showed **zero Postman test assertions**. The requests ran, but automated pass/fail assertions have not been evidenced by that run. Add Postman scripts or automated tests to assert status codes, response headers, and error bodies.

---

## Production-readiness considerations

Before treating these policies as production-ready, review the following:

- **Rate limiting:** handle Redis outages, concurrent counter initialization, trusted proxy IP resolution, and whether limits should be scoped per user or route.
- **Authentication:** establish explicitly whether JWT validation is performed at the gateway, in downstream services, or both. Keep signing configuration consistent where token validation is required.
- **Authorization:** verify role-based access on every protected operation, including write and delete methods.
- **Request validation:** validate required fields and malformed payloads at the service boundary; do not assume routing alone performs validation.
- **Error responses:** standardize error structure and status codes for gateway failures, validation errors, authentication failures, rate limits, and downstream unavailability.
- **Kafka and Saga reliability:** define retries, idempotency, duplicate handling, timeout behavior, and compensating actions.
- **Secrets and logging:** use environment-based secret configuration and redact credentials from logs.
- **Testing:** add automated assertions for routing, unauthorized requests, invalid tokens, validation failures, rate limiting, and downstream failure behavior.

These are recommendations unless a corresponding implementation and test are explicitly identified above.

## Monolith comparison

The `monolith/monolithic-app` module is retained as a separate architecture-learning example. In a monolith, business modules are deployed together in one application. In the microservices system described in this README, business capabilities are split across independently running services and communicate through HTTP and, where configured, messaging infrastructure.

For the primary project workflow, start with the API Gateway, the required business services, Kafka, and Redis rather than the monolith.

## Technologies

- Java and Spring Boot
- Maven
- Spring Web / REST APIs
- Spring Cloud Gateway (current gateway configuration)
- JWT-based authentication in the Product Service
- Apache Kafka
- Redis
- OpenTelemetry-related observability configuration
- Postman
- Git and GitHub

## Contributing / maintenance notes

When changing a route, topic, port, event schema, Saga step, or security policy, update this README and the relevant service-level documentation. Keep the README aligned with the actual source code and test evidence; distinguish verified behavior from planned improvements.
