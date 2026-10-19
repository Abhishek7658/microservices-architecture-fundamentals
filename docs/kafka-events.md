
# Kafka Event Contracts — Day 1

**Status:** Initial source-based inventory and contract decisions  
**Review date:** 19 October 2026  
**Scope:** Kafka topic relationships documented for the current microservices implementation.

This document records the event topics, producer/consumer relationships, and reliability requirements identified during the Day 1 service-boundary review.

**Important:** This is not a formal schema registry, a guarantee of runtime delivery, or proof that all event flows have passed integration tests. Payloads, configuration, and runtime behavior must be verified before these contracts are treated as release guarantees.

## 1. Topic inventory

| Topic | Producer | Consumer(s) | Contract status |
|---|---|---|---|
| `order-created` | Order Service outbox and/or event publisher; active publication path requires verification | Inventory Service, Notification Service | Payload schema and duplicate-publication risk require review. |
| `inventory-reserved` | Inventory Service | Payment Service | Reservation-success payload and required fields need documentation. |
| `inventory-reservation-failed` | Inventory Service | Order Service | Failure reason and stable order/product identifiers need documentation. |
| `payment-completed` | Payment Service | Order Service, Inventory Service | Payload compatibility across both consumers needs verification. |
| `payment-failed` | Payment Service | Inventory Service | Failure details and retry/idempotency behavior need documentation. |
| `inventory-released` | Inventory Service | Order Service | Release outcome and duplicate-processing behavior need documentation. |

Do not rename topics or change payload fields without checking every producer and consumer.

## 2. Logical event flow

1. Order Service creates an order and records an outbox event.
2. The configured publication path publishes `order-created`.
3. Inventory Service consumes `order-created` and publishes `inventory-reserved` or `inventory-reservation-failed`.
4. Payment Service consumes `inventory-reserved` and publishes `payment-completed` or `payment-failed`.
5. Order Service consumes payment and inventory outcome events to update order state.
6. Inventory Service consumes payment outcomes to finalize or release inventory.
7. Notification Service consumes `order-created`.

This is the intended flow inferred from the source inventory. Confirm the active publisher, listener configuration, topic configuration, and runtime behavior before considering the flow verified.

## 3. Event schema requirements

Before formalizing payloads, inspect the actual producer and consumer DTOs and agree on a common event-envelope strategy.

### Recommended metadata

| Field | Purpose | Decision/status |
|---|---|---|
| `eventId` | Unique identity for deduplication and tracing | Recommended; implementation requires verification. |
| `eventType` | Identifies the event type | Recommended; implementation requires verification. |
| `eventVersion` | Supports explicit schema evolution | Recommended; implementation requires verification. |
| `occurredAt` | Records when the business event occurred | Recommended; timestamp format and timezone convention need agreement. |
| `correlationId` | Connects related HTTP requests and asynchronous work | Recommended; confirm propagation through Kafka headers or payload. |

Use one documented convention for correlation metadata. If both headers and payload fields are used, define their relationship and precedence.

### Kafka record key

Use a stable key, preferably the order or relevant aggregate ID when ordering related events matters.

Document the chosen key for every topic. A key alone does not guarantee global ordering; ordering is scoped to a Kafka partition.

### Payload compatibility

For each event, document:

- Required and optional fields.
- Field types, nullability, and validation rules.
- Identifier formats and amount representation.
- Event identity, version, and correlation conventions.
- Producer and consumer ownership.
- Compatibility rules for adding, changing, or removing fields.

Money values should use a decimal-safe representation. Confirm the actual serialization format before defining the final wire schema.

## 4. Consumer reliability requirements

### Idempotency

Consumers must tolerate duplicate delivery. Use a stable event identity and a durable deduplication or equivalent business-level idempotency strategy where required.

Repeated processing of the same event must not reserve or release stock twice, initiate duplicate payments, or apply the same order transition repeatedly.

### Failure handling

A processing failure must not be treated as successful merely because the exception was logged.

For every listener, verify:

- What happens when deserialization fails.
- What happens when business processing or a database operation fails.
- Whether the listener retries, blocks, or recovers.
- When the Kafka offset is acknowledged or committed.
- Whether retry exhaustion sends the record to a dead-letter topic.
- How failed records are inspected and replayed safely.

The actual retry and dead-letter behavior must be documented from configuration and verified by tests. This document does not assert that these mechanisms are already implemented.

### Database and event consistency

Where a database update and outgoing event must agree, use a transactional outbox or another proven atomic publication strategy.

For the Order Service outbox, verify that:

1. The order and outbox record are committed consistently.
2. Pending events are retried after publication failures.
3. Published state is recorded only after the configured publication success condition.
4. Duplicate publication is safe for consumers.
5. The outbox processor and any direct publisher cannot publish the same logical event unintentionally.

For Inventory and Payment, review database changes and outgoing event publication for failure windows between the two operations.

## 5. Contract verification checklist

- [ ] Confirm the actual producer and consumer for all six topics.
- [ ] Inspect DTOs and serializers to document the current payload fields.
- [ ] Verify Kafka topic names, consumer groups, keys, and header conventions.
- [ ] Decide on the event-envelope and versioning approach.
- [ ] Verify `correlationId` propagation across HTTP and Kafka.
- [ ] Test duplicate event delivery.
- [ ] Test consumer exceptions and verify retry/recovery behavior.
- [ ] Verify dead-letter handling if configured.
- [ ] Verify outbox retry behavior and publication acknowledgment handling.
- [ ] Check for duplicate `order-created` publication paths.
- [ ] Test order, payment, and inventory state consistency under failures.
- [ ] Store actual test results and evidence in the project documentation.

## 6. Day 1 decisions and open items

### Documented requirements

- Topic names and ownership must be reviewed before contract changes.
- Consumers must be safe under duplicate delivery.
- Failure handling must not silently acknowledge unsuccessful processing.
- Retry and dead-letter behavior must be explicit and tested.
- Database changes and outgoing events require a reliable consistency strategy.

### Proposed decisions — not yet confirmed as implemented

- Standard event metadata: `eventId`, `eventType`, `eventVersion`, `occurredAt`, and `correlationId`.
- Stable Kafka record keys based on the relevant aggregate ID.
- Explicit payload schemas and compatibility rules.
- Verified retry, recovery, and dead-letter behavior.
- Durable idempotency for event consumers.

These proposals should be adopted only after reviewing the current code and deciding on the required implementation changes.

## 7. Validation status

This inventory is a source-based documentation deliverable. It does not establish that all six topics have been exercised successfully, that schemas are registered, or that retries and dead-letter handling work in the running environment.

Record the actual build, integration-test, and runtime verification results once MySQL, Redis, Kafka, and the required services are available.
