# Payment Service - Microservices Demo

## Overview

Payment Service is a Kafka consumer microservice that demonstrates:

- **Event-Driven Architecture** (consumes OrderCreatedEvent)
- **CQRS Pattern** using backend-core components
- **Retry Mechanism** with exponential backoff
- **Idempotent Processing** to prevent duplicate payments
- **Payment Gateway Simulation**

## Architecture

```
┌──────────────────────────────────────────┐
│         ms-payment-service                │
│                                           │
│  ┌─────────────────────────────────┐     │
│  │   Kafka Consumer (Listener)     │     │
│  └────────────┬────────────────────┘     │
│               │                           │
│               ▼                           │
│  ┌─────────────────────────────────┐     │
│  │   Event Handler                 │     │
│  └────────────┬────────────────────┘     │
│               │                           │
│               ▼                           │
│  ┌─────────────────────────────────┐     │
│  │   CQRS (Command Bus)            │     │
│  └────────────┬────────────────────┘     │
│               │                           │
│               ▼                           │
│  ┌─────────────────────────────────┐     │
│  │   Payment Processing            │     │
│  └────────────┬────────────────────┘     │
│               │                           │
│               ▼                           │
│  ┌─────────────────────────────────┐     │
│  │   PostgreSQL (Payments DB)      │     │
│  └─────────────────────────────────┘     │
└──────────────────────────────────────────┘
```

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **PostgreSQL** (database)
- **Kafka** (event streaming)
- **Liquibase** (database migrations)
- **Lombok** (reduce boilerplate)

## Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Gradle 8+

### Run Locally

1. **Start Dependencies (PostgreSQL, Kafka):**

   ```bash
   docker-compose up -d payments-db kafka zookeeper
   ```

2. **Run the Service:**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

### Run with Docker

```bash
docker-compose up --build
```

## Event Consumption

### Consumes: OrderCreatedEvent

From topic: `order.events`

```json
{
  "orderId": "uuid",
  "userId": "uuid",
  "totalAmountValue": 59.98,
  "currencyCode": "USD",
  "timestamp": "2024-01-01T12:00:00Z",
  "correlationId": "uuid"
}
```

### Processing Flow

1. Receive `OrderCreatedEvent` from Kafka
2. Check idempotency (payment already exists?)
3. Create `CreatePaymentCommand`
4. Execute command via CommandBus
5. Process payment via payment gateway (simulated)
6. Save payment to database
7. Acknowledge Kafka message

### Retry Strategy

- **Max Attempts:** 3
- **Initial Backoff:** 1 second
- **Backoff Multiplier:** 2.0
- **DLQ:** Enabled (failed messages go to `order.events.dlq`)

## Configuration

Key configuration in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payments_db
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: payment-service-group
      auto-offset-reset: earliest
      enable-auto-commit: false

payment:
  messaging:
    order-events-topic: "order.events"

backend-core:
  messaging:
    kafka-wrapper:
      retry:
        max-attempts: 3
        initial-backoff: 1s
        enable-dlq: true
```

## Database Schema

### Payments Table

- `id` - Primary key (UUID)
- `order_id` - Order reference (UNIQUE)
- `user_id` - User who made the payment
- `amount` - Payment amount
- `method` - Payment method (CREDIT_CARD, PAYPAL, etc.)
- `status` - Payment status (PENDING, COMPLETED, FAILED)
- `gateway_transaction_id` - Transaction ID from gateway
- `gateway_response` - Response from gateway
- Audit fields (created_at, updated_at, created_by, updated_by, version)

## Payment Processing

The service simulates payment gateway processing:

- **Success Rate:** 90% (configurable)
- **Processing Time:** ~500ms (simulated latency)
- **Idempotency:** Prevents duplicate payments for same order
- **Status Updates:** PENDING → COMPLETED/FAILED

## Monitoring

- **Health Check:** `http://localhost:8083/actuator/health`
- **Metrics:** `http://localhost:8083/actuator/metrics`
- **Kafka Consumer Metrics:** `http://localhost:8083/actuator/metrics/kafka.wrapper.events.consumed`
- **Prometheus:** `http://localhost:8083/actuator/prometheus`

## Testing

```bash
# Run all tests
./gradlew test

# Integration test with Kafka
./gradlew test --tests OrderEventConsumerIntegrationTest
```

## Development

### Project Structure

```
src/main/java/com/demo/payment/
├── modules/
│   ├── payment/         # Payment domain, commands, handlers
│   └── consumer/        # Kafka consumers, event handlers
└── config/             # Configuration classes
```

## Troubleshooting

### Not Receiving Events

- Check Kafka topic exists: `docker exec kafka kafka-topics --list --bootstrap-server localhost:9092`
- Verify consumer group: `docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group payment-service-group`

### Database Connection Issues

- Check PostgreSQL: `docker ps | grep payments-db`
- Verify connection: `psql -h localhost -p 5434 -U postgres -d payments_db`

### Payment Processing Failures

- Check logs: `docker logs ms-payment-service`
- View DLQ messages: `docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic order.events.dlq --from-beginning`

## Complete Demo Flow

1. **Start all services:**

   ```bash
   docker-compose -f docker-compose-demo.yml up -d
   ```

2. **Create an order** (via Order Service):

   ```bash
   curl -X POST http://localhost:8081/api/v1/orders \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{
       "items": [
         {"productName": "Product 1", "quantity": 2, "price": 29.99}
       ]
     }'
   ```

3. **Payment Service automatically processes** the OrderCreatedEvent

4. **Check payment** was created:
   ```bash
   docker exec payments-db psql -U postgres -d payments_db -c "SELECT * FROM payments;"
   ```

## License

Apache 2.0
