# Microservices Architecture Fundamentals

A Java/Spring Boot learning project demonstrating an API Gateway, independently running business services, synchronous REST calls, Kafka-based event processing, Redis-backed request controls and idempotency, resilience patterns, and distributed tracing. The repository also contains a separate monolithic application for comparison.

> **Source-of-truth note:** This README was prepared by reviewing the supplied project archive, service configuration, Maven files, infrastructure Compose files, and existing documentation. It distinguishes observed local behavior from architecture intent. Confirm active configuration and test results after changing the project.

## Contents

- [Architecture](#architecture)
- [Repository layout](#repository-layout)
- [Services and local ports](#services-and-local-ports)
- [Technology stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Local infrastructure](#local-infrastructure)
- [Database configuration](#database-configuration)
- [Run the services](#run-the-services)
- [API examples](#api-examples)
- [Resilience and idempotency](#resilience-and-idempotency)
- [Kafka and asynchronous processing](#kafka-and-asynchronous-processing)
- [Observability and troubleshooting](#observability-and-troubleshooting)
- [Testing](#testing)
- [Security and production-readiness notes](#security-and-production-readiness-notes)
- [Documentation and evidence](#documentation-and-evidence)
- [Known limitations](#known-limitations)

## Architecture

```text
                         Client / Postman
                                |
                                v
                       API Gateway :8080
                 routing, request limits, logging
                    /       |       |       \
                   v        v       v        v
             User :8090  Order :8082 Product :8081 Payment :8084
                            |
                            | synchronous user lookup
                            v
                         User :8090

                  Order / Inventory / Payment / Notification
                                |
                                v
                         Apache Kafka :9092

      Redis :6379      MySQL databases      OpenTelemetry Collector
      rate limit,      per-service data      :4317 / :4318
      idempotency                                   |
                                                    v
                                              Jaeger UI :16686
```

The diagram is a high-level view. The exact services participating in a request depend on the route and event flow. The synchronous order-creation trace observed during local testing contained the Gateway, Order Service, and User Service. Asynchronous Kafka processing can appear in separate traces.

### Main design ideas

- **API Gateway:** a common entry point for client requests and cross-cutting request policies.
- **Service boundaries:** User, Product, Order, Payment, Inventory, and Notification are separate Spring Boot applications.
- **Synchronous communication:** Order Service calls User Service to verify user information.
- **Asynchronous communication:** Kafka topics connect event producers and consumers in the order/inventory/payment/notification workflow.
- **Resilience:** bounded timeouts, retries, a circuit breaker, and controlled error responses are used around the Order-to-User dependency.
- **Idempotency:** Order creation uses an `Idempotency-Key` and Redis state to handle repeated requests.
- **Observability:** correlation IDs in logs and OpenTelemetry/Jaeger tracing help investigate activity across services.
- **Monolith comparison:** `monolith/monolithic-app` is a separate learning example, not the entry point for the microservices workflow.

## Repository layout

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
├── monolith/
│   └── monolithic-app/
├── infrastructure/
│   └── Kafka/
│       └── docker-compose.yml
├── observability/
│   ├── docker-compose.yml
│   └── otel-collector-config.yaml
├── docs/
│   ├── contracts/
│   │   └── order-api.yaml
│   ├── evidence/
│   ├── architecture-trust-boundaries.mmd
│   ├── architecture-preview.md
│   ├── kafka-events.md
│   └── service-boundaries.md
├── Documentation/
│   └── Task and day-by-day reports
├── README.md
├── README_microservices.md
└── README_Days_1_to_5.md
```

Each microservice has its own `pom.xml` and Maven wrapper. The repository does not have a root Maven aggregator `pom.xml` in the reviewed archive, so build or run a service from its own directory.

## Services and local ports

| Application / dependency | Port | Purpose |
|---|---:|---|
| API Gateway | `8080` | Client entry point and route policies |
| Product Service | `8081` | Product API |
| Order Service | `8082` | Order creation and lifecycle |
| Notification Service | `8083` | Kafka-driven notification handling |
| Payment Service | `8084` | Payment workflow handling |
| Inventory Service | `8085` | Inventory/stock workflow handling |
| User Service | `8090` | User lookup and user operations |
| Redis | `6379` | Gateway rate-limit state and Order Service idempotency state |
| Kafka | `9092` | Asynchronous messaging |
| Jaeger UI | `16686` | Trace search and inspection |
| OpenTelemetry Collector | `4317` / `4318` | OTLP gRPC / HTTP ingestion |

These are the local ports found in the reviewed configuration. Check the relevant `application.properties`, `application.yml`, active profile, and environment variables if a port differs on your machine.

## Technology stack

- Java (the service Maven files specify Java 25)
- Spring Boot (the service Maven files specify Spring Boot `4.1.1`)
- Maven Wrapper
- Spring Web MVC / REST APIs
- Spring Cloud Gateway Server Web MVC
- Spring Data JPA / Hibernate and MySQL
- Spring Security and JWT-related Product Service code
- Apache Kafka
- Redis
- Resilience4j
- OpenTelemetry Collector and Jaeger
- Postman for manual API checks
- Docker Compose for Kafka and observability infrastructure

## Prerequisites

Install and configure:

1. A JDK compatible with the Java version specified by the service POMs.
2. Docker Desktop with Docker Compose.
3. MySQL, with the databases listed in [Database configuration](#database-configuration).
4. Git.
5. Postman or another HTTP client.

Check Java and Docker before starting:

```powershell
java -version
docker --version
docker compose version
```

## Local infrastructure

Run the commands from the repository root in PowerShell.

### Start Kafka

```powershell
docker compose -f .\infrastructure\Kafka\docker-compose.yml up -d
```

This Compose file defines a single-node Apache Kafka 4.1.1 development broker at `localhost:9092`. It is a local development setup, not a production Kafka cluster.

### Start observability infrastructure

```powershell
docker compose -f .\observability\docker-compose.yml up -d
```

The observability Compose file starts Jaeger and an OpenTelemetry Collector. The collector receives OTLP over ports `4317` and `4318` and exports traces to Jaeger. The Compose file mounts `observability/otel-collector-config.yaml`.

Open Jaeger at:

- `http://localhost:16686`

### Start or inspect Redis

Redis is not defined in the two Compose files above; it was run separately during the local verification. Before creating a container, check whether Redis is already running:

```powershell
docker ps
```

If Redis is not running and port `6379` is free, a local development container can be started with:

```powershell
docker run -d --name redis -p 6379:6379 redis:7
```

If a container named `redis` already exists, start or reuse it rather than creating a duplicate. Confirm connectivity with:

```powershell
docker exec redis redis-cli ping
```

Expected response:

```text
PONG
```

The Order Service and Gateway use Redis for different purposes: the Order Service uses it for idempotency, and the Gateway uses it for rate-limit counters.

## Database configuration

The service configurations reference these MySQL databases:

| Service | Database |
|---|---|
| User Service (dev profile) | `user_service_dev_db` |
| Order Service (dev profile) | `order_service_db` |
| Product Service | `productdb` |
| Inventory Service | `inventorydb` |
| Payment Service | `paymentdb` |

The repository archive does not provide a single database bootstrap script covering all of these databases. Create the databases and users required by the active service configuration before starting the applications.

The User Service and Order Service development profiles read `DB_USERNAME` and `DB_PASSWORD` from environment variables. Set them in the terminal or environment used to launch the services. For example, in PowerShell:

```powershell
$env:DB_USERNAME = "your-local-db-user"
$env:DB_PASSWORD = "your-local-db-password"
```

Use your own local development credentials; do not commit credentials to Git. Some other service property files in the archive contain literal database credentials and JWT-related configuration. Review and move secrets to environment variables or a secret manager before sharing or deploying the project. Do not copy secrets into this README.

Several services use `spring.jpa.hibernate.ddl-auto=update` and SQL logging in local configuration. This is convenient for development, but production deployments should use controlled database migrations and carefully managed logging.

## Run the services

Each service is an independent Maven project. Open a separate terminal per service and run the commands from the relevant module directory.

For example, start the User Service:

```powershell
cd .\microservices\user-service
.\mvnw.cmd spring-boot:run
```

Start the Order Service:

```powershell
cd .\microservices\order-service
.\mvnw.cmd spring-boot:run
```

Start the Product Service:

```powershell
cd .\microservices\product-service
.\mvnw.cmd spring-boot:run
```

Start the Payment Service:

```powershell
cd .\microservices\payment-service
.\mvnw.cmd spring-boot:run
```

Start the Inventory Service:

```powershell
cd .\microservices\inventory-service
.\mvnw.cmd spring-boot:run
```

Start the Notification Service:

```powershell
cd .\microservices\notification-service
.\mvnw.cmd spring-boot:run
```

Start the API Gateway after the target services and Redis are ready:

```powershell
cd .\microservices\api-gateway
.\mvnw.cmd spring-boot:run
```

These are standard Maven Wrapper launch commands for the modules in the archive; they have not been executed as part of generating this README. Wait for each application's startup-complete log before testing. If a service fails, inspect its active profile, port, database credentials, and Kafka/Redis availability.

### Build and test one service

From a service directory:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test-compile
.\mvnw.cmd test
```

Run these commands per module as needed. A successful test run in one service does not mean all services' tests have passed.

## API examples

### Create an order

The public Order API is exposed through the Gateway. The Gateway rewrites `/api/orders` to the Order Service controller path `/api/v1/orders`.

**Request**

```http
POST http://localhost:8080/api/orders
Content-Type: application/json
Idempotency-Key: order-demo-001
```

```json
{
  "userId": 1,
  "productId": 1,
  "productName": "Laptop",
  "quantity": 1,
  "amount": 65000
}
```

The `Idempotency-Key` header is required by the documented contract. Use a new key for a new test; reuse the same key only when intentionally testing idempotent replay.

**Observed local behavior:** a valid order-creation request returned `201 Created`. An order with `quantity: 0` returned `400 Bad Request` with the message `quantity: must be greater than or equal to 1`. When the User Service was unavailable, the Order API returned `503 Service Unavailable`.

### Gateway routes found in configuration

| Gateway path | Target | Routing behavior |
|---|---|---|
| `/api/users/**` | User Service `localhost:8090` | Forwarded to User Service |
| `/api/orders/**` | Order Service `localhost:8082` | Rewrites `/api/orders` to `/api/v1/orders` |
| `/api/products` and `/api/products/**` | Product Service `localhost:8081` | Rewrites to `/api/v1/products` |
| `/api/v2/products` and `/api/v2/products/**` | Product Service `localhost:8081` | Separate v2 route; inspect backend controller mapping before use |
| `/api/payments/**` | Payment Service `localhost:8084` | Rewrites the gateway prefix to `/payments` |

A configured Gateway route does not guarantee that every backend endpoint exists, is healthy, or allows every HTTP method. Consult each service controller and security configuration for the complete endpoint contract.

The Order API contract is documented in `docs/contracts/order-api.yaml`.

## Resilience and idempotency

### Order-to-User Service call

The Order Service development configuration includes:

- User Service base URL: `http://localhost:8090`
- Connect timeout: `2000 ms`
- Read timeout: `2000 ms`
- Maximum retry attempts: `3`
- Initial backoff: `500 ms`
- Resilience4j circuit breaker with a 50% failure threshold, minimum five calls, a ten-call sliding window, a ten-second open-state wait, and three permitted half-open calls
- `UserNotFoundException` excluded from circuit-breaker failure counting

Retries can help with transient errors but may increase latency and downstream load. A circuit breaker can reduce repeated calls to an unhealthy dependency. Verify actual retry counts and breaker transitions from logs or Actuator endpoints before treating them as evidence for a particular run.

### Redis-backed idempotency

Order creation uses an `Idempotency-Key` and Redis keys prefixed with `idempotency:order:` with a 24-hour TTL. The payload fingerprint includes `userId`, `productId`, `productName`, `quantity`, and `amount`.

The intended behavior is:

- A first request claims the key for processing.
- A repeated request with the same key and same payload returns the stored response.
- Reusing the same key with a different payload returns `409 Conflict`.
- The reservation is cleaned up on transaction rollback.

**Known consistency limitation:** Redis and MySQL do not share a single atomic transaction. A database commit followed by a failed Redis write can leave a committed order without a cached response, or leave a processing reservation until its TTL expires. A database-backed idempotency record or reconciliation strategy would provide stronger guarantees.

### Transactional outbox

The Order Service contains an outbox processor for publishing `ORDER_CREATED` events to Kafka's `order-created` topic. The outbox pattern reduces the risk of committing an order in the database while losing the corresponding event. It does not guarantee exactly-once delivery: a crash after Kafka accepts a message but before the outbox record is marked published can cause a duplicate publication. Consumers should therefore be idempotent.

## Kafka and asynchronous processing

The local Kafka broker is configured through `infrastructure/Kafka/docker-compose.yml`. The repository's event inventory in `docs/kafka-events.md` lists these topics and intended relationships:

| Topic | Intended producer | Intended consumer(s) |
|---|---|---|
| `order-created` | Order Service outbox / publisher | Inventory Service, Notification Service |
| `inventory-reserved` | Inventory Service | Payment Service |
| `inventory-reservation-failed` | Inventory Service | Order Service |
| `payment-completed` | Payment Service | Order Service, Inventory Service |
| `payment-failed` | Payment Service | Inventory Service |
| `inventory-released` | Inventory Service | Order Service |

Treat this as the documented event inventory; confirm the active publisher, payload fields, consumer configuration, retry/acknowledgment behavior, and dead-letter handling in source code before relying on those as production guarantees.

For event-driven workflows, consider duplicate delivery, consumer retries, ordering by Kafka partition key, compensation behavior, and the difference between a successful publish acknowledgment and successful downstream processing.

## Observability and troubleshooting

### Correlation IDs and logs

Service console logging is configured to include a `correlationId` MDC field in several services. The Gateway response has been observed returning an `X-Correlation-ID` header. Use that ID to find related log entries across the synchronous request and associated processing where propagation is configured.

A correlation ID and a Jaeger trace ID are distinct identifiers. Do not assume they have the same value.

### Jaeger and OpenTelemetry

Start the observability stack using:

```powershell
docker compose -f .\observability\docker-compose.yml up -d
```

Open `http://localhost:16686` and search by service, operation, or trace ID. The Collector accepts OTLP gRPC on `4317` and OTLP HTTP on `4318`, then exports traces to Jaeger according to `observability/otel-collector-config.yaml`.

During local verification, a successful order HTTP trace was observed with 21 spans across `api-gateway`, `order-service`, and `user-service`. Separate asynchronous outbox/event-processing traces were also observed. This is evidence from that run, not a guarantee that every request or every registered service appears in one trace.

### Useful troubleshooting checks

```powershell
docker ps
docker exec redis redis-cli ping
```

- Confirm the service process has started and is listening on its configured port.
- Check MySQL connectivity and database credentials.
- Check Redis when idempotency or rate limiting behaves unexpectedly.
- Check Kafka connectivity and service logs when event consumers do not process messages.
- Search Jaeger using the trace ID; search console logs using the correlation ID.
- Use the trace and logs together: a trace can show span timing while logs may provide business-level context.

The Gateway Actuator URL and `http.server.requests` endpoint returned `404` during a previous optional check. Do not assume Gateway metrics are exposed at those paths; inspect the current dependencies, endpoint exposure, and routing/security configuration if metrics become a requirement.

## Testing

Manual Postman scenarios observed during local verification include:

| Scenario | Observed result |
|---|---|
| Valid order request | `201 Created` |
| Order with quantity `0` | `400 Bad Request` |
| User Service stopped | `503 Service Unavailable` |
| User Service restarted | Subsequent order request returned `201 Created` |
| Gateway rate-limit run | `429 Too Many Requests` observed in a 101-iteration run |
| Idempotency replay | Same key/payload returned the original result in prior local verification |
| Idempotency key reused with changed payload | `409 Conflict` in prior local verification |

The rate-limit Postman script originally accepted only `200` and `429`; valid `201` responses caused assertion failures. For a dedicated rate-limit test, assert `429` specifically after the threshold is exceeded. Manual Postman results do not replace automated integration tests.

For repeatable validation, run each module's test suite and record the command, result, and environment. Do not report tests as passing unless they were run against the current source state.

## Security and production-readiness notes

This repository is a local learning/development project. Before production use:

- Move literal passwords and signing secrets from service property files to environment variables or a secrets manager; rotate any credentials that have been committed to a shared repository.
- Do not trust arbitrary forwarded client-IP headers when applying rate limits behind a proxy.
- Define the expected behavior if Redis is unavailable.
- Confirm where JWT authentication is performed (Gateway, service, or both) and verify authorization on every protected operation.
- Replace `ddl-auto=update` with controlled schema migrations for production.
- Avoid logging passwords, bearer tokens, signing keys, or sensitive payload fields.
- Configure Kafka consumer retries, acknowledgment semantics, dead-letter handling, and idempotent business processing.
- Review outbox processing for concurrent instances and duplicate event delivery.
- Add automated integration tests for routes, authorization, invalid payloads, timeouts, rate limiting, and partial outages.
- Review CORS, request-size limits, safe errors, and service network exposure for the deployment environment.

These are recommendations and review items, not claims that every production control is already implemented.

## Documentation and evidence

- `docs/service-boundaries.md` — service responsibilities and boundaries.
- `docs/contracts/order-api.yaml` — Order API contract.
- `docs/kafka-events.md` — Kafka topic inventory and contract/reliability notes.
- `docs/architecture-trust-boundaries.mmd` — trust-boundary diagram.
- `docs/architecture-preview.md` and `docs/evidence/` — architecture preview and request evidence.
- `Documentation/` — day-by-day and task-specific reports.
- `README_microservices.md` — earlier detailed microservices README.
- `README_Days_1_to_5.md` — summary of the first five project work days.

## Monolith comparison

`monolith/monolithic-app` is a separate Spring Boot example included for architecture comparison. The main workflow described in this README uses the applications under `microservices/` and their supporting infrastructure.

## Contributing and maintenance

When changing a route, service port, database configuration, Kafka topic, event payload, resilience policy, or security rule:

1. Update the relevant service configuration and tests.
2. Update the API/event contract and architecture notes.
3. Run tests for the affected module(s).
4. Capture the actual verification result.
5. Review `git diff` and commit only intentional changes.

Keep documentation aligned with the current code. Clearly label recommendations and intended flows separately from behavior verified in a specific run.
