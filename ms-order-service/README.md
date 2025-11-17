# Order Service - Microservices Demo

## Overview

Order Service is a microservice that demonstrates:

- **Clean Architecture** with proper layer separation
- **CQRS Pattern** using backend-core components
- **Event-Driven Architecture** via Kafka
- **Feign Client** for authentication
- **Database Migrations** with Liquibase

## Architecture

```
┌──────────────────────────────────────────┐
│         ms-order-service                  │
│                                           │
│  ┌─────────────────────────────────┐     │
│  │    REST API (Controller)        │     │
│  └────────────┬────────────────────┘     │
│               │                           │
│               ▼                           │
│  ┌─────────────────────────────────┐     │
│  │    CQRS (Command/Query Bus)     │     │
│  └────────────┬────────────────────┘     │
│               │                           │
│               ▼                           │
│  ┌─────────────────────────────────┐     │
│  │    Domain Layer (Business Logic)│     │
│  └────────────┬────────────────────┘     │
│               │                           │
│               ▼                           │
│  ┌─────────────────────────────────┐     │
│  │    Infrastructure (DB, Kafka)   │     │
│  └─────────────────────────────────┘     │
└──────────────────────────────────────────┘
```

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **PostgreSQL** (database)
- **Kafka** (event streaming)
- **Liquibase** (database migrations)
- **Feign Client** (HTTP client)
- **Lombok** (reduce boilerplate)
- **OpenAPI/Swagger** (API documentation)

## Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Gradle 8+

### Run Locally

1. **Start Dependencies (PostgreSQL, Kafka):**

   ```bash
   docker-compose up -d orders-db kafka zookeeper
   ```

2. **Run the Service:**

   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

3. **Access Swagger UI:**
   ```
   http://localhost:8081/swagger-ui.html
   ```

### Run with Docker

```bash
docker-compose up --build
```

## API Endpoints

### Create Order

```bash
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "items": [
    {
      "productName": "Product 1",
      "description": "Description",
      "quantity": 2,
      "price": 29.99
    }
  ]
}
```

### Get Order by ID

```bash
GET /api/v1/orders/{orderId}
```

### Get User Orders

```bash
GET /api/v1/orders/user/{userId}
```

### Cancel Order

```bash
DELETE /api/v1/orders/{orderId}?reason=Customer request
Authorization: Bearer <token>
```

## Configuration

Key configuration in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders_db
  kafka:
    bootstrap-servers: localhost:9092

order:
  messaging:
    order-events-topic: "order.events"

services:
  auth-service:
    url: http://localhost:8082
```

## Database Schema

### Orders Table

- `id` - Primary key (UUID)
- `user_id` - User who created the order
- `total_amount` - Order total
- `currency` - Currency code
- `status` - Order status (PENDING, CONFIRMED, etc.)
- Audit fields (created_at, updated_at, created_by, updated_by, version)

### Order Items Table

- `id` - Primary key (UUID)
- `order_id` - Foreign key to orders
- `product_name` - Product name
- `quantity` - Quantity ordered
- `unit_price` - Price per unit
- `total_price` - Total price

## Events Published

### OrderCreatedEvent

Published to `order.events` topic when an order is created.

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

## Monitoring

- **Health Check:** `http://localhost:8081/actuator/health`
- **Metrics:** `http://localhost:8081/actuator/metrics`
- **Prometheus:** `http://localhost:8081/actuator/prometheus`

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests CreateOrderCommandHandlerTest
```

## Development

### Project Structure

```
src/main/java/com/demo/order/
├── domain/              # Domain models & business logic
├── application/         # CQRS commands, queries, handlers
├── infrastructure/      # DB, Kafka, Feign implementations
├── interfaces/          # REST controllers
└── config/             # Configuration classes
```

## Troubleshooting

### Database Connection Issues

- Check PostgreSQL is running: `docker ps | grep orders-db`
- Verify connection: `psql -h localhost -p 5433 -U postgres -d orders_db`

### Kafka Issues

- Check Kafka is running: `docker ps | grep kafka`
- View topics: `docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list`

## License

Apache 2.0
