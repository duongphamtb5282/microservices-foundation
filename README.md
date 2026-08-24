# Microservices Foundation

A production-oriented microservices platform built with **Java 21**, **Spring Boot 3**, **Kafka**,
**Keycloak**, and the full reliability toolbox: transactional outbox, saga choreography with
compensation, at-least-once delivery with consumer idempotency, layered retry, circuit breakers,
and bulkhead isolation at the boundaries.

> **Last updated: 2026-08-25** — port map corrected (gateway `8088`, Keycloak `8080`), Boot version
> split documented (3.5.5 vs 3.2.0), reliability-pattern section rewritten to match the live code
> (ADR-0001/0005/0006/0007/0010), Keycloak-only auth (ADR-0003) replaces the old "dual auth" claim.

## Contents

- [Architecture](#-architecture)
- [Services](#-services)
- [Event-Driven Backbone](#-event-driven-backbone)
- [Reliability Patterns](#-reliability-patterns) (outbox · inbox/idempotency · saga · retry · circuit breaker · bulkhead)
- [Tracing & Correlation](#-tracing--correlation)
- [Quick Start](#-quick-start)
- [Testing](#-testing)
- [Documentation & ADRs](#-documentation--adrs)
- [Known Issues & Tech Debt](#-known-issues--tech-debt)

## 🏗️ Architecture

```
                          ┌──────────────┐
                          │   Client     │
                          └──────┬───────┘
                                 │ HTTP/REST
                                 ▼
                     ┌───────────────────────┐
                     │    API Gateway        │  :8088  (Spring Cloud Gateway, WebFlux)
                     │  - Routes / auth      │
                     │  - Rate limiter       │  resilience4j ratelimiter per route
                     │  - Circuit breaker    │  order-service / payment-service routes → /fallback 503
                     │  - JWT + API-key      │  protected routes
                     └──────┬───────┬────────┘
                            │       │
              ┌─────────────▼──┐   ┌▼──────────────────┐
              │   Auth Service │   │  Customer Service │  :8084  (WebFlux, MongoDB)
              │   :8082        │   │  - reactive CRUD  │
              │   (Keycloak    │   │  - WebSocket      │
              │    proxy)      │   └────────┬──────────┘
              └───────┬───────┘            │
                      │ user_outbox        │ consumes user-events
                      │ (transactional)    │ (dedup on email)
                      ▼                    ▼
              ┌───────────────────────────────────────────┐
              │                 Kafka (KRaft)              │
              │  topics:                                   │
              │   user-events    auth ──► customer         │
              │   order.events   order ──► payment         │  ORDER_CREATED / ORDER_CANCELLED
              │   payment.events payment ──► order         │  PaymentResultEvent (saga return)
              │   order.commands in-process command bus    │
              └───────▲──────────────┬─────────────────────┘
                      │              │
       ┌──────────────┴──┐   ┌───────▼────────────┐
       │  Order Service  │   │  Payment Service   │
       │  :8081          │   │  :8083 (Kafka-only)│
       │  CQRS, Feign→   │   │  outbox relay      │
       │  auth, CB       │   │  DLQ for bad msgs  │
       └──────┬──────────┘   └───────┬────────────┘
              │                      │
        ┌─────▼─────┐          ┌─────▼─────┐
        │ PostgreSQL│          │ PostgreSQL│        ┌─────────────┐
        │ orders_db │          │ payments_db│       │ Keycloak    │
        └───────────┘          └───────────┘       │ :8080       │
                                                   │ realm:      │
  auth-service: PostgreSQL (auth), Redis (multi-tier cache)       │ auth-service│
  ms-customer:  MongoDB (customerdb), Redis                        └─────────────┘
```

**Auth model (ADR-0003):** auth-service is a **thin Keycloak proxy** — registration and token
management are delegated to Keycloak, which is the single user store. JWT validation happens at the
**gateway** (JWKS). Services add **no security filter chains** (S-06).

## 📦 Services

| Service | Port | Boot | Stack | Sync surface | Async role |
|---|---|---|---|---|---|
| **api-gateway** | 8088 | 3.5.5 | Spring Cloud Gateway, WebFlux | all `/api/*` entry | — |
| **auth-service** | 8082 | 3.5.5 | Boot Web, PostgreSQL, Redis, Keycloak client | login/register/refresh/me, `/api/users`, `/api/roles`, `/api/cache` | **produces** `user-events` via transactional outbox |
| **ms-order-service** | 8081 | 3.2.0 | Boot Web, PostgreSQL, Liquibase, Feign→auth, resilience4j | `/api/v1/orders`, `/api/v2/orders` | produces `order.events`, consumes `payment.events` |
| **ms-payment-service** | 8083 | 3.2.0 | Boot Web, PostgreSQL, Kafka | **no REST** (Kafka-only) | consumes `order.events`, produces `payment.events` via outbox |
| **ms-customer** | 8084 | 3.2.0 | WebFlux, MongoDB, WebSocket, Redis | `/api/customers` | consumes `user-events` |

Foundation libraries:

- **backend-core** — shared platform library: Kafka wrapper + CQRS command bus, correlation/tracing
  filters, multi-tier cache (Caffeine L1 + Redis L2), `CircuitBreakerService`, `SecurityService`
  (AES-GCM field encryption), outbox primitives, OpenAPI default. *Consumed as a fileTree jar in
  dev — carries no transitive metadata, so consumers declare resilience4j/security/caffeine
  themselves (see each `build.gradle`).*
- **shared** — event/command contracts (single source of truth, ADR-0004): `UserCreatedEvent`,
  `OrderCreatedEvent`, `PaymentResultEvent`, etc.

## 🔄 Event-Driven Backbone

All async communication goes through Kafka (KRaft, `auth-service/docker-compose.yml`). Delivery is
**at-least-once**; consumers are idempotent (see [Inbox & Idempotency](#inbox--idempotency)).

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `user-events` | auth (outbox relay) | ms-customer `UserEventConsumer` | `UserCreatedEvent` |
| `order.events` | ms-order (CQRS bus) | ms-payment `OrderEventConsumer` | `OrderCreatedEvent` / `OrderCancelledEvent` (dispatched on `eventType`) |
| `payment.events` | ms-payment (outbox relay) | ms-order `PaymentEventConsumer` | `PaymentResultEvent` |
| `order.commands` | in-process command bus | in-process handlers | commands (ADR-0007) |

Serialization notes (fixed 2026-08-25):

- Producers write `__TypeId__` type headers (JsonSerializer default, re-enabled in core after they
  were briefly disabled — disabling them broke every consumer with *"Error deserializing VALUE ...
  no type information in headers"*).
- Consumers decode via the header; services may set `spring.kafka.consumer.value-default-type`
  as a fallback for headerless records (ms-customer does: `UserCreatedEvent` on `user-events`).

## 🛡️ Reliability Patterns

### Transactional Outbox (ADR-0001, ADR-0006)

**What:** a business transaction and its event publish are committed **atomically** — the event is
written to an outbox table in the same DB transaction, and a relay publishes it afterwards. No
dual-write inconsistency, no lost events.

**Where (live):**

- `auth-service`: `user_outbox` (auth schema) — written by `UserRegistrationService` after the
  Keycloak call succeeds; relayed by `UserOutboxRelay` to `user-events`.
- `ms-payment-service`: `payment_outbox` (payments schema) — written with the payment row; relayed
  to `payment.events`.

**Lifecycle:** `PENDING` → publish → `PUBLISHED` (+`published_at`) → purged after retention (7d);
`FAILED` after `max-attempts` (5). Relays poll every 500 ms; auth backoff grows 1s → 5m.

### Inbox & Idempotency (ADR-0005)

**What:** the classic inbox table (processed-message ledger beside the consumer's data) exists in
this platform in **deliberately lightweight form**: consumers achieve idempotency with domain-key
deduplication instead of a message-level inbox.

- ms-customer deduplicates `UserCreatedEvent` **on email** — re-delivery cannot create duplicate
  customers.
- Acknowledgment discipline (`MANUAL_IMMEDIATE` + ack only after success) gives at-least-once
  without the inbox: failure leaves the offset uncommitted and Kafka redelivers.
- Poison messages are handled per consumer: payment classifies unparseable payloads as
  non-retryable and routes them to a **DLQ** (`<topic>.dlq`, core `DeadLetterQueue`); headerless
  legacy records decode via `value-default-type`.

**Trade-off (ADR-0005):** no inbox table means no exactly-once across crashes *between* DB commit
and ack — a crash there replays the event, which the dedup key absorbs.

### Saga (ADR-0002, ADR-0007)

**What:** the order lifecycle is a **choreographed saga** across two services with an outbox at
each leg — no orchestrator, no locks:

```
1. POST /orders
2. order saved (CREATED) ──order.events: ORDER_CREATED──► 3. payment saved (outbox PENDING)
4. order settles ◄──payment.events: PaymentResultEvent── payment relay publishes (saga return path, ADR-0002)
```

**Compensation (ADR-0007):**

```
DELETE /orders/{id}?reason=...  →  order set CANCELLED
  ──order.events: ORDER_CANCELLED──►  payment refunds/cancels (missing payment is a no-op,
                                      handled as if it never existed)
```

Consumers are idempotent against replayed events, and the saga tolerates partial failure: a
cancelled order whose payment never arrived leaves no dangling payment, and vice versa.

### Retry

Retries exist at **four layers** — each with a different purpose:

| Layer | Mechanism | Bounds |
|---|---|---|
| Kafka producer | idempotent producer (`retries=3`, `acks=all`, `max.in.flight=1`) | send-level |
| Kafka consumer | at-least-once redelivery (no ack on failure; `CorrelationAwareErrorHandler` logs) | infinite redelivery — idempotency absorbs replays |
| Outbox relay | attempts with exponential backoff (1s → 5m), `FAILED` after 5 | finite, then manual DLQ/repair |
| DLQ | non-retryable errors (unparseable payloads) → `<topic>.dlq` via core `DeadLetterQueue` | off the hot path |

### Circuit Breaker (ADR-0010)

Two coordinated layers:

1. **Gateway (entry):** Spring Cloud Circuit Breaker on the `order-service-v1/v2` and
   `payment-service` routes — open state → `fallbackUri: forward:/fallback` (503). Per-instance
   config: 50% failure threshold, 10-call sliding window, 30s open state, 3 half-open probes.
2. **Service boundaries (inner):** `CircuitBreakerService` (core, programmatic
   `execute(serviceName, supplier)`) wired at the two real synchronous call sites:
   - **order → auth** (`AuthValidationService`, Feign token validation),
   - **auth → Keycloak** (`KeycloakService`, breaker `"keycloak"`).

States feed `BusinessHealthIndicator` and `CircuitBreakerAlertService`, with Prometheus alert
rules (`monitoring/alerts.yml`) on `resilience4j_circuitbreaker_state`.

### Bulkhead (ADR-0010 — Proposed)

**Design:** isolation so one subsystem's failure/load cannot exhaust another's resources.

| Boundary | Status |
|---|---|
| Entry-point bulkhead (gateway) | ✅ **de-facto live** — the gateway circuit breaker + per-route rate limiters (`auth-public` 10 r/s, `auth-protected` 100 r/s, `order-service` 50 r/s, `payment-service` 20 r/s) bound client demand before it reaches any service |
| Dedicated executors | ✅ `coreAsyncExecutor`, `outboxPublisherExecutor` (`AsyncExecutorConfiguration`) — outbox relays never run on shared pools |
| Async command dispatch isolation | ⏳ planned — bounded `ThreadPoolTaskExecutor` for `KafkaCommandBus.executeAsync` (currently common ForkJoinPool) |
| `@Bulkhead` / `ThreadPoolBulkhead` beans | ⏳ none yet — first ship when a third saga participant joins or load tests demand it (revisit trigger in ADR-0010) |

## 📡 Tracing & Correlation

- **Header:** `X-Correlation-ID` (in + echoed out); **MDC key:** `correlationId`.
- **Chain:** REST (core `CorrelationIdFilter`) and Kafka (payload `correlationId` field — the
  reliable async path; header keys kebab/camel mismatch is bridged by the payload fallback).
- Registering a user with `X-Correlation-ID` threads that id through the outbox payload into
  ms-customer's log line.
- **Known gaps:** only ms-payment prints `%X{correlationId}` in its log pattern; the gateway
  passes the header through but doesn't generate one; no Zipkin endpoint configured.

> Full setup + test recipes: `docs/AUTH_SERVICE_GATEWAY_TESTING(important).md` §9.

## 🚀 Quick Start

### Prerequisites

Java 21 · Docker & Docker Compose · MongoDB (customer) · jq

### 1. Infrastructure

```bash
cd auth-service && docker compose up -d        # postgres, redis, kafka (KRaft), keycloak
cd ms-customer && docker compose up -d         # mongodb
```

Keycloak: realm `auth-service`, client `auth-service-client`; seed users `admin`/`admin123`,
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
Redis keys): `docs/AUTH_SERVICE_GATEWAY_TESTING(important).md` §10.

## 🧪 Testing

- **Hands-on API guide for every service** (auth users/roles/cache, customer, order v1/v2,
  payment-via-Kafka saga), swagger links, error scenarios, tracing tests, env-var guide:
  **`docs/AUTH_SERVICE_GATEWAY_TESTING(important).md`**.
- **Swagger UI:** `http://localhost:8088/swagger-ui.html` (proxies order) ·
  `:8082/swagger-ui.html` (auth) · `:8081` (order) · `:8083` (payment — empty, no REST) ·
  `:8084` (customer). OpenAPI JSON at `/v3/api-docs` per service.
- **Correlation & logging tests:** `docs/adding_correlationid_in_service(sync and async).md`.
- **Monitoring:** `monitoring/deploy-monitoring.sh` → Prometheus + Grafana (`:3000` admin/admin);
  metrics at `/actuator/prometheus` on all services.
- Integration tests per module: `./gradlew test` (Testcontainers).

## 📚 Documentation & ADRs

| Doc | What |
|---|---|
| [AUTH_SERVICE_GATEWAY_TESTING(important).md](docs/AUTH_SERVICE_GATEWAY_TESTING(important).md) | **The** testing playbook: every API, swagger, tracing, env vars |
| [MICROSERVICES_DEMO_SOLUTION.md](docs/MICROSERVICES_DEMO_SOLUTION.md) | Order & payment demo, monitoring stack |
| [COMPLETE_ARCHITECTURE_OVERVIEW.md](docs/COMPLETE_ARCHITECTURE_OVERVIEW.md) | Deep-dive architecture |
| [KAFKA_WRAPPER_IMPLEMENTATION_SUMMARY.md](docs/KAFKA_WRAPPER_IMPLEMENTATION_SUMMARY.md) | Kafka CQRS wrapper, retry/DLQ |
| [DEVELOPER_QUICK_REFERENCE.md](docs/DEVELOPER_QUICK_REFERENCE.md) · [CODE_FORMATTING_AND_STYLE_GUIDE.md](docs/CODE_FORMATTING_AND_STYLE_GUIDE.md) | Dev workflow, quality gates |

**ADRs** (`docs/adr/`): 0001 transactional outbox · 0002 saga return path · 0003 Keycloak-only
identity · 0004 gateway-local JWT validation · 0005 consumer idempotency without inbox · 0006
outbox in auth-service · 0007 saga compensation (cancel/refund) · 0008 virtual threads on blocking
services · 0009 DB transaction hygiene + pool standardization · 0010 resilience isolation /
bulkhead · 0011 concurrency boundaries · 0012 exception-handling rules.

## ⚠️ Known Issues & Tech Debt

- **Boot version split:** auth + gateway on 3.5.5; order, payment, customer on 3.2.0 (older
  springdoc/feign/resilience4j line). Works, but unify before upgrading shared infra.
- **Gateway → order routes 404:** `StripPrefix=1` on `/api/v1/orders/**` routes vs controllers
  mapping `/api/v1/orders` — test order APIs directly on 8081 until fixed (one-liner:
  `StripPrefix=0`).
- **Redis timeout keys:** ms-customer binds `spring.redis.timeout` (non-standard, ignored by Boot
  auto-config) and ms-order hardcodes `2000ms` — `REDIS_TIMEOUT` only truly works in auth
  (see testing doc §10.3).
- **Stale `.env` files:** auth `SERVER_PORT=8083`, gateway `SERVER_PORT=8080` — both disagree with
  the live defaults (8082 / 8088).
- **`application-template.yml`** files advertise an older var scheme than the live ymls.
- **DLQ is payment-only:** customer's `customer-events.dlq` topic is declared but unwired (its
  consumer TODO); order has no DLQ.
- **fileTree jar dependency:** backend-core carries no transitive metadata to consumers — a
  composite build or `publishToMavenLocal` is the durable fix.
- **Stale core after rebuild:** consumers snapshot the core jar at process start. With `bootRun`
  the classpath is built when Gradle launches, so a rebuilt core jar only takes effect after a
  service restart; with `bootJar`/`java -jar` the core jar is *embedded* in the fat jar at build
  time, so a core change requires rebuilding every consumer fat jar. Symptom: `NoClassDefFoundError`
  for a class that clearly exists in `backend-core/build/libs` (2026-08-25:
  `RetryContext$RetryContextBuilder` crashed payment's consumer on the first ORDER_CREATED event).

---

**Last Updated:** 2026-08-25 · **Version:** 1.0.0
