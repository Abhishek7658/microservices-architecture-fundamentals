
# Day 1 — Service Boundaries and Contract Design

**Status:** Source-based architecture review — 19 October 2026  
**Scope:** Existing implementation under `microservices/`

This document records service responsibilities, HTTP entry points, observed event relationships, trust boundaries, and API contract gaps. Static source review does not establish that every end-to-end flow works at runtime.

## 1. Service ownership and HTTP entry points

| Service | Observed controller path(s) | Responsibility | Entry point / notes |
|---|---|---|---|
| API Gateway | Configured routes in `microservices/api-gateway/src/main/resources/application.properties` | External HTTP routing, rate limiting, and correlation-ID handling | Port 8080. Routes to User (8090), Order (8082), Product (8081), and Payment (8084). |
| User Service | `/api/users` | User creation, retrieval, update, and deletion | Gateway forwards `/api/users/**` without a rewrite. |
| Order Service | `/api/v1/orders` | Order lifecycle and coordination with User Service and asynchronous events | Gateway rewrites `/api/orders/**` to `/api/v1/orders/**`. Creation requires `Idempotency-Key`. |
| Product Service | `/products`, `/api/v1/products`, `/api/v2/products` | Product catalogue and versioned product APIs | Gateway routes `/api/products/**` to v1 and `/api/v2/products/**` to v2. |
| Payment Service | `/payments` | Payment operations and payment outcome events | Gateway rewrites `/api/payments/**` to `/payments`. |
| Inventory Service | No public route identified in the Gateway configuration reviewed | Inventory reservation and release based on events | Event-driven interactions are documented below; runtime wiring still requires verification. |
| Notification Service | `/notifications` | Notification operations and order-created event consumption | No direct Notification route is configured in the Gateway properties reviewed. |

**Routing note:** Gateway routing configuration documents how requests are forwarded; it does not prove that authentication or authorization is enforced on every route. Direct access to service ports must also be considered when assessing the trust boundary.

## 2. Critical order flow

The following is the source-derived flow. It is not a claim that the complete workflow has passed an end-to-end runtime test.

1. A client sends `POST /api/orders` through the API Gateway.
2. The Gateway rewrites the request to `POST /api/v1/orders` on Order Service.
3. Order Service requires `Idempotency-Key`, validates `CreateOrderRequest`, and calls User Service to verify the referenced user.
4. Order Service persists the order and an outbox event. The outbox processing path and the separate direct publisher must be checked to establish which publication path is active and whether duplicate publication is possible.
5. Inventory Service consumes `order-created` and publishes reservation outcomes.
6. Payment Service consumes `inventory-reserved` and publishes payment outcomes.
7. Order Service consumes payment and inventory outcome events to update order state. Inventory Service consumes payment outcomes to finalize or release stock.
8. Notification Service consumes `order-created`.

### Runtime verification still required

- Confirm which outbox processor publishes pending events and how it marks them published.
- Confirm whether the direct `OrderEventPublisher` is invoked and whether it duplicates outbox publication.
- Verify Kafka retry, acknowledgment, and dead-letter behavior.
- Verify order-state transitions under duplicate delivery and consumer failures.
- Capture a distributed trace if the local tracing setup is available.

## 3. Kafka topic inventory

Topic names and producer/consumer relationships below are recorded from the existing event inventory. Payload schemas and delivery guarantees must be verified against the actual producers, listeners, and Kafka configuration.

| Topic | Producer | Consumer(s) | Contract work required |
|---|---|---|---|
| `order-created` | Order Service outbox and/or event publisher; active path requires verification | Inventory Service, Notification Service | Document `OrderCreatedEvent`; align amount representation and define event identity, version, and timestamp conventions. |
| `inventory-reserved` | Inventory Service | Payment Service | Document reservation-success payload and required fields. |
| `inventory-reservation-failed` | Inventory Service | Order Service | Document failure reason and stable order/product identifiers. |
| `payment-completed` | Payment Service | Order Service, Inventory Service | Align `PaymentCompletedEvent` payload across consumers; document transaction reference and decimal amount representation. |
| `payment-failed` | Payment Service | Inventory Service | Document failure reason, retry behavior, and idempotency requirements. |
| `inventory-released` | Inventory Service | Order Service | Document release outcome and duplicate-processing behavior. |

### Event metadata decision

Before changing event payloads, agree whether to use a common event envelope or add metadata to each event.

Recommended minimum metadata:

- `eventId`
- `eventType`
- `eventVersion`
- `occurredAt`
- `correlationId`

Choose one documented convention for correlation metadata: Kafka headers, payload, or a clearly defined combination. Use a consistent Kafka record key, preferably the order or aggregate ID where ordering is required.

### Consumer reliability requirements

- Consumers must tolerate duplicate delivery and be safe under redelivery.
- A processing failure must not be converted into a successful acknowledgment merely because it was logged.
- Retry, backoff, acknowledgment, and dead-letter recovery behavior must be configured and tested.
- Where a database update and outgoing event must agree, use a transactional outbox or another proven atomic publication strategy.

## 4. Trust boundaries

### TB1 — External client to API Gateway

Client-controlled HTTP headers and payloads enter the system here. Validate request data and treat inbound correlation IDs as untrusted input.

Authentication and authorization policy must be verified against the actual security configuration. A configured route does not, by itself, prove JWT validation or authorization enforcement.

### TB2 — API Gateway to internal HTTP services

The Gateway forwards requests to service endpoints. Do not assume service ports are inaccessible directly unless network and deployment configuration enforce that restriction.

### TB3 — Service-to-service HTTP

Order Service calls User Service synchronously. Timeouts, retry policy, circuit-breaker behavior, correlation propagation, and downstream error translation belong to this boundary.

### TB4 — Services to Kafka broker

Kafka introduces asynchronous delivery, possible redelivery, and independent consumer failures. Consumers must handle duplicate events safely, and failures must reach the configured retry and recovery mechanisms.

### TB5 — Services to databases

Each service should own its database schema and write path. Cross-service database writes should be avoided; services should coordinate through APIs or events.

### TB6 — Gateway/Order Service to Redis

The Gateway uses Redis for rate-limit counters, while Order Service uses Redis for idempotency state. Redis state is not automatically part of the relational database transaction. Failure behavior, expiration, and consistency between Redis and database state require explicit decisions and tests.

## 5. API contract and design gaps

1. Maintain version-controlled OpenAPI specifications for externally reachable HTTP APIs.
2. Standardize error-response fields and document actual behavior for validation errors, missing resources, and downstream failures.
3. Use request/response DTOs instead of exposing persistence entities directly, particularly where Payment or Product endpoints currently return entities.
4. Add explicit constraints for nonblank strings, positive identifiers, valid quantities, and nonnegative decimal amounts. Use a decimal-safe representation for money.
5. Document Kafka payload schemas, event identity/version, record keys, headers, producer/consumer ownership, and retry/dead-letter behavior.
6. Test both successful behavior and negative/boundary cases, including request validation and a downstream or event-processing failure.
7. Verify that the outbox processor and direct event publisher cannot publish the same logical event unintentionally.
8. Document the existing API-versioning differences, including Product v1/v2 and the unversioned Payment path.
9. Verify authentication and authorization at both the Gateway and direct service entry points.

## 6. Day 1 validation evidence

### Verified HTTP behavior

| Scenario | Observed result | Status |
|---|---|---|
| Valid order creation through the Gateway | `201 Created` | Observed in Postman |
| Order creation with `quantity: -1`, before the exception-handler fix | `500 Internal Server Error` | Initial defect observed |
| Order creation with `quantity: -1`, after the exception-handler fix | `400 Bad Request` with validation message, path, and trace ID | Observed in Postman |

Retain the screenshots in `docs/evidence/` with descriptive names, for example:

- `order-create-201.png`
- `order-invalid-quantity-400.png`

The error-response screenshot should show the response body as well as the status code.

### Remaining validation

This is a source-based architecture review with limited HTTP runtime evidence. Do not claim that the entire multi-service workflow, Kafka delivery guarantees, tracing, or all API contracts have passed runtime tests until those checks have been performed.

Record actual build/test commands and their results after running them in the developer environment with the required MySQL, Redis, Kafka, and service dependencies available.

## 7. Execution and review notes

1. Review the service routes and event relationships against the current source.
2. Validate each OpenAPI specification and resolve references.
3. Run the successful and negative HTTP tests and preserve their evidence.
4. Verify the architecture diagram and critical-flow sequence diagrams against the actual service configuration.
5. Record unresolved implementation gaps rather than silently documenting proposed behavior as implemented.
6. Commit the Day 1 documentation and contract changes after review.
