# Microservices Foundation

A production-oriented microservices platform built with **Java 21** and **Spring Boot 3** — a
complete e-commerce order lifecycle (identity → orders → payments → customer profiles) running on
**Kafka (KRaft)** with a **GraphQL backend-for-frontend (BFF)** at the edge.

The platform is engineered for the problems that kill microservices in production: **guaranteed
event delivery**, **exactly-once business effects from at-least-once messaging**, **saga
compensation**, **boundary isolation under load**, and **one aggregated API surface** for frontend
clients. Every reliability mechanism is *live and exercised*, not scaffolded.

> **Last updated: 2026-08-25** — GraphQL BFF on the gateway, MFA/SSO in the Keycloak realm,
> resilience & hardening pass (single-responsibility restructure, service-layer transactions,
> status-code retry boundaries, lazy bean init, cache policy, shared contract versioning,
> repository query standard, field-diff audit trail) — 409/503 error semantics, Dockerfiles on
> temurin 21.

## Contents

- [Why This Platform](#-why-this-platform)
- [Architecture](#-architecture) (C4 context + container)
- [Services](#-services)
- [Event-Driven Backbone](#-event-driven-backbone)
- [GraphQL BFF](#-graphql-bff)
- [Reliability Patterns](#-reliability-patterns) (outbox · idempotency · saga · retry · circuit breaker · bulkhead · error semantics)
- [Security](#-security) (JWT at the edge · Keycloak · MFA & SSO · field encryption)
- [Observability](#-observability) (correlation · metrics · audit trail)
- [Caching](#-caching) (multi-tier Caffeine + Redis)
- [Quick Start](#-quick-start)
- [Testing](#-testing)
- [Documentation](#-documentation)
- [Known Issues & Tech Debt](#-known-issues--tech-debt)

## 🚀 Why This Platform

| Problem in a naive microservices setup | What this platform does about it |
|---|---|
| Event published outside the DB transaction → events lost on crash | **Transactional outbox** — the event row commits with the business write; a relay publishes it after |
| At-least-once Kafka redelivery → duplicate customers, double charges | **Domain-key idempotency** — consumers dedup on email / upsert by `orderId` / replay idempotency keys |
| Distributed order↔payment flow with no rollback story | **Choreographed saga** with compensation — cancel always returns the money, even across crashes |
| Downstream outage takes down the caller's threads | **Circuit breakers + bulkheads at every boundary** — gateway rate limits, command-dispatch isolation, 503 semantics |
| Frontend fan-out to five REST services, N round-trips per screen | **GraphQL BFF on the gateway** — one `POST /graphql`, aggregated queries, one JWT checked once |
| Credentials & identity scattered per service | **Keycloak as the single identity provider** — OIDC, MFA (TOTP), SSO (GitHub/Google), JWT validated at the edge |
| Microservices that are impossible to debug across boundaries | **Single correlation id** threaded through REST *and* Kafka, field-level audit trail, Prometheus metrics |

The result: a foundation where teams add services without re-solving delivery, consistency,
isolation, or observability — the platform provides the rails.

## 🏗️ Architecture

### C4 Context

![C4-Context](docs/architecture/c4-context.svg)

*Editable source: [c4-context.drawio](docs/architecture/c4-context.drawio)*

Clients reach the platform through a single HTTP entry point. The system owns identity (Keycloak),
the event backbone (Kafka), three relational stores (PostgreSQL per service), customer profiles
(MongoDB), cache/rate-limit state (Redis), and its own monitoring (Prometheus + Grafana).

### C4 Container

![C4-Container](docs/architecture/c4-container.svg)

*Editable source: [c4-container.drawio](docs/architecture/c4-container.drawio)*

Five services, one gateway, zero shared databases. **api-gateway** terminates REST + GraphQL and
validates JWTs at the edge; **auth-service** is a thin Keycloak proxy owning users, roles and
permissions; **ms-order-service** owns the order lifecycle (CQRS command bus); **ms-payment-service**
is deliberately Kafka-only — money movement is event-driven, with no REST surface to abuse;
**ms-customer** is reactive (WebFlux + MongoDB) and self-provisions customer profiles from
`user-events`. All services link the **backend-core** and **shared** foundation jars (fileTree) for
the platform machinery: Kafka wrapper, command bus, multi-tier cache, circuit breakers, outbox
primitives, audit listener, and the shared event/command contracts.

## 📦 Services

| Service | Port | Boot | Stack | Sync surface | Async role |
|---|---|---|---|---|---|
| **api-gateway** | 8088 | 3.5.5 | Spring Cloud Gateway, WebFlux, GraphQL BFF, Redis, springdoc | all `/api/*` entry + `POST /graphql` | — |
| **auth-service** | 8082 | 3.5.5 | Boot Web, PostgreSQL, Redis, Keycloak client | login/register/refresh/me, `/api/users`, `/api/roles`, `/api/permissions`, forgot/reset-password, `/api/cache` | **produces** `user-events` via transactional outbox |
| **ms-order-service** | 8081 | 3.2.0 | Boot Web, PostgreSQL, Liquibase, Feign→auth, resilience4j | `/api/v1/orders`, `/api/v2/orders` | produces `order.events`, consumes `payment.events` |
| **ms-payment-service** | 8083 | 3.2.0 | Boot Web, PostgreSQL, Kafka | **no REST** (Kafka-only) | consumes `order.events`, produces `payment.events` via outbox |
| **ms-customer** | 8084 | 3.2.0 | WebFlux, MongoDB, WebSocket, Redis | `/api/customers` | consumes `user-events` |

Foundation libraries:

- **backend-core** — shared platform library: Kafka wrapper + CQRS command bus, correlation/tracing
  filters, multi-tier cache (Caffeine L1 + Redis L2), `CircuitBreakerService`, `SecurityService`
  (AES-GCM field encryption), outbox primitives, audit entity listener, resilience isolation
  (bulkhead beans), OpenAPI default. *Consumed as a fileTree jar in dev — carries no transitive
  metadata, so consumers declare resilience4j/security/caffeine themselves (see each
  `build.gradle`).*
- **shared** — event/command contracts (single source of truth): `UserCreatedEvent`,
  `OrderCreatedEvent`, `PaymentResultEvent`, etc.

## 🔄 Event-Driven Backbone

All async communication goes through Kafka (KRaft, `auth-service/docker-compose.yml`). Delivery is
**at-least-once**; consumers are idempotent (see [Inbox & Idempotency](#inbox--idempotency)).

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `user-events` | auth (outbox relay) | ms-customer `UserEventConsumer` | `UserCreatedEvent` |
| `order.events` | ms-order (CQRS bus) | ms-payment `OrderEventConsumer` | `OrderCreatedEvent` / `OrderCancelledEvent` (dispatched on `eventType`) |
| `payment.events` | ms-payment (outbox relay) | ms-order `PaymentEventConsumer` | `PaymentResultEvent` |
| `order.commands` | in-process command bus | in-process handlers | commands |

Serialization notes:

- Producers write `__TypeId__` type headers (JsonSerializer default, re-enabled in core after they
  were briefly disabled — disabling them broke every consumer with *"Error deserializing VALUE ...
  no type information in headers"*).
- Consumers decode via the header; services may set `spring.kafka.consumer.value-default-type`
  as a fallback for headerless records (ms-customer does: `UserCreatedEvent` on `user-events`).

## ⚡ GraphQL BFF

The gateway hosts a **backend-for-frontend**: one aggregated `POST /graphql` endpoint (`:8088`)
that frontend clients use instead of fanning out to five REST services.

**Schema** (`api-gateway/src/main/resources/graphql/schema.graphqls`) — queries: `me`, `order`,
`orders` (paginated, filtered by status), `customer`, `paymentByOrder`; mutations: `login`,
`refreshToken`, `logout`, `createOrder`, `cancelOrder`.

**Security model:** `login`/`refreshToken`/`logout` are public; every other operation requires
`Authorization: Bearer <jwt>`. The BFF validates the token with the **same local JWKS validator**
as the gateway's `AuthenticationFilter` and forwards it to each downstream service, which validates
the token itself — a compromised BFF still cannot mint access.

**Details:**

- `createOrder` honors an optional `idempotencyKey` — the same key replays the stored result (200)
  instead of creating a second order.
- Amounts are strings (`BigDecimal`) — never floats — to preserve money precision.
- Downstream base URLs are configurable (`BFF_ORDER_URL` / `BFF_CUSTOMER_URL` /
  `BFF_PAYMENT_URL` / `BFF_AUTH_URL`, defaults `localhost:8081/8084/8083/8082`).
- The BFF's `BffHttpClient` forwards the `X-Correlation-ID` to every downstream call.

## 🛡️ Reliability Patterns

### Transactional Outbox

**What:** a business transaction and its event publish are committed **atomically** — the event is
written to an outbox table in the same DB transaction, and a relay publishes it afterwards. No
dual-write inconsistency, no lost events.

**Where (live):**

- `auth-service`: `user_outbox` (auth schema) — written by `UserRegistrationService` after the
  Keycloak call succeeds; relayed by `UserOutboxRelay` to `user-events`. A failed Keycloak call
  rolls back the local write.
- `ms-payment-service`: `payment_outbox` (payments schema) — written with the payment row; relayed
  to `payment.events`.

**Lifecycle:** `PENDING` → publish → `PUBLISHED` (+`published_at`) → purged after retention (7d);
`FAILED` after `max-attempts` (5). Relays poll every 500 ms; auth backoff grows 1s → 5m.

### Inbox & Idempotency

Consumers achieve idempotency with **domain-key deduplication** — a deliberately lightweight
alternative to a message-level inbox table:

- ms-customer deduplicates `UserCreatedEvent` **on email** — re-delivery cannot create duplicate
  customers.
- ms-payment **upserts** payments by `orderId` — a replayed `ORDER_CREATED` overwrites rather than
  duplicates.
- ms-order keeps an **idempotency-key table** for `POST /api/v2/orders`: a completed key replays the
  stored result (200), an in-flight key returns **409 CONFLICT**.
- Acknowledgment discipline (`MANUAL_IMMEDIATE` + ack only after success) gives at-least-once
  without the inbox: failure leaves the offset uncommitted and Kafka redelivers.
- Poison messages are handled per consumer: payment classifies unparseable payloads as
  non-retryable and routes them to a **DLQ** (`<topic>.dlq`, core `DeadLetterQueue`); headerless
  legacy records decode via `value-default-type`.

**Trade-off:** no inbox table means no exactly-once across crashes *between* DB commit and ack — a
crash there replays the event, which the dedup key absorbs.

### Saga

The order lifecycle is a **choreographed saga** across two services with an outbox at each leg — no
orchestrator, no locks:

```
1. POST /orders
2. order saved (CREATED) ──order.events: ORDER_CREATED──► 3. payment saved (outbox PENDING)
4. order settles ◄──payment.events: PaymentResultEvent── payment relay publishes (saga return path)
```

**Compensation:**

```
DELETE /orders/{id}?reason=...  →  order set CANCELLED
  ──order.events: ORDER_CANCELLED──►  payment refunds/cancels (missing payment is a no-op,
                                      handled as if it never existed)
```

Consumers are idempotent against replayed events, and the saga tolerates partial failure: a
cancelled order whose payment never arrived leaves no dangling payment, and vice versa. Order rows
carry an `@Version` for **optimistic locking** — concurrent writers get a retryable 409, not a lost
update.

### Retry

Retries exist at **four layers** — each with a different purpose:

| Layer | Mechanism | Bounds |
|---|---|---|
| Kafka producer | idempotent producer (`retries=3`, `acks=all`, `max.in.flight=1`) | send-level |
| Kafka consumer | at-least-once redelivery (no ack on failure; `CorrelationAwareErrorHandler` logs) | infinite redelivery — idempotency absorbs replays |
| Outbox relay | attempts with exponential backoff (1s → 5m), `FAILED` after 5 | finite, then manual DLQ/repair |
| DLQ | non-retryable errors (unparseable payloads) → `<topic>.dlq` via core `DeadLetterQueue` | off the hot path |

**DLQ self-healing:** payment's `reconcileFailedRows()` scheduled job re-attempts `FAILED` outbox
rows every hour (`payment.outbox.reconcile-interval`, default `PT1H`) — a transient failure that
recovered is re-published automatically instead of waiting for a human. Order's consumer has
core-wrapper DLQ enabled (`enableDlq: true`, suffix `.dlq`).

### Circuit Breaker

Two coordinated layers:

1. **Gateway (entry):** Spring Cloud Circuit Breaker on the `order-service-v1/v2`,
   `payment-service`, and `customer-service` routes — open state → `fallbackUri: forward:/fallback`
   (503). Per-instance config: 50% failure threshold, 10-call sliding window, 30s open state, 3
   half-open probes.
2. **Service boundaries (inner):** `CircuitBreakerService` (core, programmatic
   `execute(serviceName, supplier)`) wired at the two real synchronous call sites:
   - **order → auth** (`AuthValidationService`, Feign token validation),
   - **auth → Keycloak** (`KeycloakService`, breaker `"keycloak"`).

States feed `BusinessHealthIndicator` and `CircuitBreakerAlertService`, with Prometheus alert rules
(`monitoring/alerts.yml`) on `resilience4j_circuitbreaker_state`.

### Bulkhead

Isolation so one subsystem's failure/load cannot exhaust another's resources:

| Boundary | Status |
|---|---|
| Entry-point bulkhead (gateway) | ✅ **live** — gateway circuit breakers + per-route rate limiters (`auth-public` 10 r/s, `auth-protected` 100 r/s, `order v1/v2` 50 r/s, `payment` 20 r/s, `customer` 30 r/s, all keyed on the client remote address) bound client demand before it reaches any service |
| Async command dispatch isolation | ✅ **live** — `ResilienceIsolationConfiguration` (backend-core) exposes a semaphore bulkhead `coreCommandSemaphoreBulkhead` (64 permits, 200 ms wait) and a thread-pool bulkhead `coreCommandThreadPoolBulkhead` (4 core / 8 max threads, queue capacity 100, `CallerRunsPolicy`); `KafkaCommandBus.executeAsync` dispatches through the thread-pool bulkhead |
| Saturated bulkhead → 503 | ✅ `CommandFailureStatus` maps `BUSY` → 503 SERVICE_UNAVAILABLE (retryable) |
| Dedicated executors | ✅ `coreAsyncExecutor`, `outboxPublisherExecutor` (`AsyncExecutorConfiguration`) — outbox relays never run on shared pools |

### Error Semantics

Commands flow through `KafkaCommandBus` → `CommandFailureStatus` (order REST):

| Code | HTTP | Meaning |
|---|---|---|
| `CONFLICT` | 409 | idempotency key claimed by an in-flight request, or optimistic-lock conflict on a concurrent write |
| `BUSY` | 503 | command bulkhead saturated — retryable |
| everything else | 400 | client error (invalid order, access denied, validation) |

- `OrderControllerAdvice` maps commit-time lock exceptions
  (`ObjectOptimisticLockingFailureException`, `OptimisticLockException`) to **409
  CONCURRENT_MODIFICATION** — distinct from the idempotency-key `CONFLICT` so clients can tell
  "retry later" apart from "the same key is still running".
- Missing/invalid required headers and path variables are 400s, not 500s (a missing Authorization
  header on `POST /orders` must not surface as INTERNAL_ERROR).
- Unexpected exceptions propagate to `GlobalExceptionHandler` / `OrderControllerAdvice` — no inline
  catch-all `500` blocks.

## 🔐 Security

- **Identity is Keycloak's job.** auth-service is a thin Keycloak proxy — registration and token
  management are delegated to Keycloak, which is the single user store. JWT validation happens at
  the **gateway** (JWKS) and in the **GraphQL BFF** (same validator). Services add **no security
  filter chains** except auth-service itself.
- **MFA (TOTP):** realm-level `CONFIGURE_TOTP` required action + OTP policy (`HmacSHA1`, 6 digits,
  30 s period) — imported automatically from the realm config, zero code changes. Enroll via the
  Keycloak admin console → the next login shows a QR code for Google Authenticator.
- **SSO:** realm-level identity providers for **GitHub** and **Google** (placeholders in the realm
  JSON; paste real client ids/secrets into the Keycloak admin console to activate).
- **Field-level encryption:** backend-core's `SecurityService` encrypts sensitive fields
  (AES-GCM) via `EncryptedString` / `DataEncryptionService` — data at rest is protected beyond the
  database's own encryption.
- **API-key authentication** at the gateway alongside JWT for machine-to-machine entry.
- Secrets never live in the repo — the realm JSON carries placeholders only.

Full MFA/SSO setup + end-to-end test recipes: `docs/AUTH_SERVICE_GATEWAY_TESTING(important)` §13.

## 📡 Observability

- **Correlation:** `X-Correlation-ID` (in + echoed out); MDC key `correlationId`. The gateway's
  `WebFluxCorrelationFilter` is the sanctioned generator — it accepts a well-formed inbound
  `X-Correlation-ID` (5–100 chars) or generates one, echoes it on the response, and the GraphQL BFF
  forwards it to every downstream call.
- **Chain:** REST (core `CorrelationIdFilter`, gateway `WebFluxCorrelationFilter`) and Kafka
  (payload `correlationId` field — the reliable async path; header key kebab/camel mismatch is
  bridged by the payload fallback, `MDCUtil`/`CorrelationAwareConsumer` in core). Registering a user
  with `X-Correlation-ID` threads that id through the outbox payload into ms-customer's log line.
- **Metrics:** all services expose `/actuator/prometheus`; `monitoring/deploy-monitoring.sh` brings
  up Prometheus + Grafana with alert rules on circuit-breaker state, outbox failures and
  business counters (`BusinessMetricsService`), plus `BusinessHealthIndicator` for service health.
- **Audit trail:** `audit_log` table with a same-transaction listener in backend-core — a
  human-readable old→new changelog for Role, Permission, User and Customer, with a 90-day retention
  purge.

> Full setup + test recipes: `docs/AUTH_SERVICE_GATEWAY_TESTING(important)` §9.

## 🧠 Caching

Multi-tier cache in backend-core (Caffeine L1 + Redis L2) with per-cache TTLs on both tiers:

- **auth-service:** roles/permissions served from `@Cacheable` service methods; write-through
  eviction on every mutation; `/api/cache` endpoints for manual reload.
- **ms-order:** order-by-id cached 5m.
- **ms-customer:** customer profile cached 5m (Caffeine wrapped in Mono, reactive-friendly).

Write-through policy: every mutation evicts/updates the cache in the same request path, so reads
never serve stale data after a write.

## 🚀 Quick Start

### Prerequisites

Java 21 · Docker & Docker Compose · MongoDB (customer) · jq

### 1. Infrastructure

```bash
cd auth-service && docker compose up -d        # postgres, redis, kafka (KRaft), keycloak
cd ms-customer && docker compose up -d         # mongodb
```

Keycloak: realm `auth-service`, client `auth-service-client`, admin console
`http://localhost:8080` (`admin`/`admin123`); seed users `admin`/`admin123`,
`testuser`/`password123` (imported from `auth-service/docker/keycloak/realm-config/`).

### 2. Build (order matters — fileTree jar deps)

```bash
cd backend-core && ./gradlew jar        # 1st — services link its build/libs/*.jar
cd ../shared     && ./gradlew jar       # 2nd — event/command contracts
# then each service (any order):
cd ../auth-service && ./gradlew bootRun &       # 8082
cd ../api-gateway  && ./gradlew bootRun &       # 8088
cd ../ms-order-service && ./gradlew bootRun &   # 8081
cd ../ms-payment-service && ./gradlew bootRun & # 8083
cd ../ms-customer && ./gradlew bootRun &        # 8084
```

### 3. Smoke test

```bash
curl -s http://localhost:8088/actuator/health | jq .status            # gateway
curl -s http://localhost:8082/actuator/health | jq .status            # auth
curl -s http://localhost:8081/actuator/health | jq .status            # order
curl -s http://localhost:8083/actuator/health | jq .status            # payment
curl -s http://localhost:8084/actuator/health | jq .status            # customer

# Full flow: login → register → customer auto-created via outbox → place order → saga settles
curl -s -X POST http://localhost:8088/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"password123"}' | jq
```

### 4. Environment variables

Every setting has a `${VAR:default}` fallback — dev needs **nothing**; prod requires the
`REQUIRED` set. How env vars reach Spring (relaxed binding + placeholders), the five injection
methods, and the per-service audit (incl. the `REDIS_TIMEOUT` walkthrough and the two mis-wired
Redis keys): `docs/AUTH_SERVICE_GATEWAY_TESTING(important)` §10.

## 🧪 Testing

- **Hands-on API guide for every service** (auth users/roles/permissions/cache, MFA/SSO,
  customer, order v1/v2, payment-via-Kafka saga), swagger links, error scenarios, tracing tests,
  env-var guide: **`docs/AUTH_SERVICE_GATEWAY_TESTING(important)`**.
- **Aggregated Swagger (gateway):** `http://localhost:8088/swagger-ui.html` lists all four
  services as groups — Order Service (`/order/v3/api-docs`), Payment Service
  (`/payment/v3/api-docs`), Auth Service (`/auth/v3/api-docs`), Customer Service
  (`/customer/v3/api-docs`). Per-service consoles still work directly (`:8081/:8082/:8083/:8084`).
- **GraphQL:** `POST http://localhost:8088/graphql` — introspection enabled; `login` first, then
  query `orders` with the returned bearer token.
- **Correlation & logging tests:** `docs/adding_correlationid_in_service(sync and async)`.
- **Monitoring:** `monitoring/deploy-monitoring.sh` → Prometheus + Grafana (`:3000` admin/admin);
  metrics at `/actuator/prometheus` on all services.
- Integration tests per module: `./gradlew test` (Testcontainers).

## 📚 Documentation

| Doc (no extension) | What |
|---|---|
| `docs/AUTH_SERVICE_GATEWAY_TESTING(important)` | **The** testing playbook: every API, swagger, tracing, env vars, MFA/SSO recipes |
| `docs/MICROSERVICES_DEMO_SOLUTION` | Order & payment demo, monitoring stack |
| `docs/COMPLETE_ARCHITECTURE_OVERVIEW` | Deep-dive architecture |
| `docs/KAFKA_WRAPPER_IMPLEMENTATION_SUMMARY` | Kafka CQRS wrapper, retry/DLQ |
| `docs/DEVELOPER_QUICK_REFERENCE` · `docs/CODE_FORMATTING_AND_STYLE_GUIDE` | Dev workflow, quality gates |
| `docs/architecture/*.drawio` | C4 context/container + saga sequence diagrams (draw.io source) |
| `docs/architecture/*.svg` | Rendered C4 diagrams (what you see above) |

## ⚠️ Known Issues & Tech Debt

- **Boot version split:** auth + gateway on 3.5.5; order, payment, customer on 3.2.0 (older
  springdoc/feign/resilience4j line). Works, but unify before upgrading shared infra.
- **fileTree jar dependency:** backend-core carries no transitive metadata to consumers — a
  composite build or `publishToMavenLocal` is the durable fix.
- **Stale core after rebuild:** consumers snapshot the core jar at process start. With `bootRun`
  the classpath is built when Gradle launches, so a rebuilt core jar only takes effect after a
  service restart; with `bootJar`/`java -jar` the core jar is *embedded* in the fat jar at build
  time, so a core change requires rebuilding every consumer fat jar. Symptom: `NoClassDefFoundError`
  for a class that clearly exists in `backend-core/build/libs` (2026-08-25:
  `RetryContext$RetryContextBuilder` crashed payment's consumer on the first ORDER_CREATED event).
- **Customer DLQ unwired:** `customer-events.dlq` is declared in ms-customer config but its
  consumer has a TODO — poison user events are dropped, not dead-lettered (payment and order have
  working DLQs).
- **Log patterns:** ms-order and the gateway do not print `%X{correlationId}` (payment, customer,
  and the backend-core default pattern do) — add the pattern to those two `logging.pattern.*`
  entries.

---
