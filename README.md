# Microservices Foundation

A comprehensive, production-ready microservices foundation built with Java 21, Spring Boot 3.5, and modern architectural patterns. This project demonstrates best practices for building scalable, resilient, and maintainable microservices architectures.

## 🎯 Overview

This microservices foundation provides a complete ecosystem for building distributed systems with:

- **Event-Driven Architecture** using Apache Kafka
- **CQRS Pattern** implementation with command/query separation
- **Clean Architecture** principles across all services
- **Reactive Programming** support with Spring WebFlux
- **Comprehensive Monitoring** with Prometheus and Grafana
- **Kubernetes-Ready** deployment configurations
- **Multi-Layer Caching** (L1: Caffeine, L2: Redis)
- **Circuit Breaker** patterns with Resilience4j
- **Code Quality** enforcement with Spotless and Checkstyle

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Complete System Architecture                        │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌──────────────┐
                              │   Client     │
                              │ (Web/Mobile) │
                              └──────┬───────┘
                                     │
                                     │ HTTP/REST
                                     ▼
                          ┌────────────────────┐
                          │   API Gateway      │
                          │   (Port: 8080)     │
                          │  - Routing         │
                          │  - Load Balancing  │
                          │  - Circuit Breaker │
                          └──────────┬─────────┘
                                     │
                    ┌────────────────┼─────────────────┐
                    │                │                  │
                    ▼                ▼                  ▼
        ┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐
        │  Auth Service    │  │ Order Service│  │ Payment Service  │
        │  (Port: 8082)    │  │ (Port: 8081) │  │  (Port: 8083)    │
        │                  │  │              │  │                  │
        │  - JWT Auth      │  │ - CQRS       │  │  - CQRS          │
        │  - Keycloak      │  │ - Events     │  │  - Events        │
        │  - User Mgmt     │  │ - Commands   │  │  - Commands      │
        └──────────────────┘  └──────┼──────┘  └──────▲──────┘
                                     │                │
                                     │ Publishes      │ Consumes
                                     │ OrderCreated   │ OrderCreated
                                     ▼                │
                          ┌─────────────────────────────┐│
                          │      Kafka Cluster          ││
                          │                             ││
                          │  Topics:                    ││
                          │  ├─ order.events ───────────┘│
                          │  ├─ order.commands           │
                          │  ├─ payment.events            │
                          │  ├─ order.events.dlq         │
                          │  └─ payment.events.dlq       │
                          │                              │
                          │  Features:                   │
                          │  ├─ CQRS Pattern             │
                          │  ├─ Retry with Backoff       │
                          │  ├─ Dead Letter Queue        │
                          │  └─ Monitoring               │
                          └──────────────────────────────┘
                                     │
                    ┌────────────────┼─────────────────┐
                    ▼                ▼                  ▼
        ┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐
        │  PostgreSQL      │  │ PostgreSQL   │  │  PostgreSQL      │
        │  auth_db         │  │ orders_db    │  │  payments_db     │
        └──────────────────┘  └──────────────┘  └──────────────────┘
```

## 📦 Services

### Core Services

#### 1. **API Gateway** (`api-gateway`)
- **Port:** 8080
- **Technology:** Spring Cloud Gateway, WebFlux
- **Features:**
  - Centralized routing and load balancing
  - Circuit breaker integration
  - Request/response transformation
  - Service discovery integration

#### 2. **Auth Service** (`auth-service`)
- **Port:** 8082
- **Technology:** Spring Boot, PostgreSQL, Keycloak
- **Features:**
  - Dual authentication modes (Custom JWT & Keycloak OAuth2)
  - User management and registration
  - Token validation endpoints
  - Multi-layer caching (Caffeine + Redis)
  - Log masking for sensitive data

#### 3. **Order Service** (`ms-order-service`)
- **Port:** 8081
- **Technology:** Spring Boot, PostgreSQL, Kafka
- **Features:**
  - Clean Architecture implementation
  - CQRS pattern with backend-core
  - Event-driven order processing
  - Feign client for auth integration
  - Database migrations with Liquibase

#### 4. **Payment Service** (`ms-payment-service`)
- **Port:** 8083
- **Technology:** Spring Boot, PostgreSQL, Kafka
- **Features:**
  - CQRS pattern implementation
  - Event consumption from Order Service
  - Payment processing workflow
  - Dead letter queue handling

#### 5. **Customer Service** (`ms-customer`)
- **Port:** 8084
- **Technology:** Spring WebFlux, MongoDB, WebSocket
- **Features:**
  - Reactive programming model
  - WebSocket support for real-time updates
  - MongoDB document storage
  - CQRS with event sourcing
  - Multi-level caching

### Foundation Libraries

#### **Backend Core** (`backend-core`)
Shared foundation library providing:
- **Kafka CQRS Wrapper:** Simplified Kafka integration with retry, DLQ, and monitoring
- **Database Infrastructure:** Connection pooling, migrations, audit support
- **Caching Layer:** L1 (Caffeine) and L2 (Redis) cache configuration
- **Security Foundation:** JWT utilities, password encoding, CORS
- **Logging & Monitoring:** Structured logging, log masking, metrics
- **Common Utilities:** Date/time handling, validation, serialization

#### **Shared** (`shared`)
Common libraries and utilities:
- Shared DTOs and models
- Common exceptions and error handling
- Utility classes
- Kafka stream bindings

## 🚀 Quick Start

### Prerequisites

- **Java 21** (LTS)
- **Docker & Docker Compose** (for infrastructure)
- **Gradle 8.x** (included via wrapper)
- **PostgreSQL** (via Docker)
- **Redis** (via Docker)
- **Kafka** (via Docker)
- **Keycloak** (optional, via Docker)

### 1. Clone the Repository

```bash
git clone <repository-url>
cd microservices-foundation
```

### 2. Start Infrastructure Services

```bash
# Start all infrastructure (PostgreSQL, Redis, Kafka, Keycloak)
docker-compose up -d postgres redis kafka keycloak

# Or start individual services
docker-compose up -d postgres
docker-compose up -d redis
docker-compose up -d kafka
docker-compose up -d keycloak
```

### 3. Build All Services

```bash
# Build all services in correct order
./scripts/build-all.sh

# Or build individually
cd backend-core && ./gradlew build
cd ../auth-service && ./gradlew build
cd ../ms-order-service && ./gradlew build
cd ../ms-payment-service && ./gradlew build
cd ../ms-customer && ./gradlew build
cd ../api-gateway && ./gradlew build
```

### 4. Configure Environment Variables

Each service has an `env.template` file. Copy and configure:

```bash
# Example for auth-service
cd auth-service
cp env.template .env
# Edit .env with your configuration
```

### 5. Run Services

#### Option A: Run All Services (Development)

```bash
./scripts/start-dev.sh
```

#### Option B: Run Individual Services

```bash
# Auth Service
cd auth-service
./scripts/start-custom.sh  # or start-keycloak.sh

# Order Service
cd ms-order-service
./gradlew bootRun

# Payment Service
cd ms-payment-service
./gradlew bootRun

# Customer Service
cd ms-customer
./gradlew bootRun

# API Gateway
cd api-gateway
./gradlew bootRun
```

### 6. Verify Services

```bash
# Health checks
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8082/actuator/health  # Auth Service
curl http://localhost:8081/actuator/health  # Order Service
curl http://localhost:8083/actuator/health  # Payment Service
curl http://localhost:8084/actuator/health  # Customer Service
```

## 🧪 Testing

### Integration Tests

```bash
# Test user registration flow
./test_user_registration_integration.sh

# Test message flow between services
./test-integration-message-flow.sh

# Test demo message consumption
./test-integration/demo-message-consumption.sh
```

### Unit Tests

```bash
# Run tests for all services
./scripts/test-compilation.sh

# Run tests for specific service
cd <service-name>
./gradlew test
```

## 📊 Monitoring

### Prometheus & Grafana

```bash
# Deploy monitoring stack
cd monitoring
./deploy-monitoring.sh

# Access Grafana
# http://localhost:3000 (default: admin/admin)
```

### Metrics Endpoints

All services expose Prometheus metrics at:
```
GET /actuator/prometheus
```

### Key Metrics

- Request rates and latencies
- Error rates
- Cache hit/miss ratios
- Kafka consumer lag
- Database connection pool status
- Circuit breaker states

## 🐳 Docker Support

### Build Docker Images

```bash
# Build all Docker images
./scripts/docker-build-all.sh

# Build specific service
cd <service-name>
docker build -t <service-name>:latest .
```

### Docker Compose

Each service includes `docker-compose.yml` for local development:

```bash
cd auth-service
docker-compose up -d
```

## ☸️ Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (EKS, GKE, or local)
- `kubectl` configured
- Docker images pushed to registry

### Deploy to Kubernetes

```bash
cd eks
./deploy-all.sh

# Or deploy individual services
kubectl apply -f deployments/
kubectl apply -f services/
kubectl apply -f configmaps/
```

### Kubernetes Resources

- **Deployments:** Service deployments with resource limits
- **Services:** ClusterIP and LoadBalancer services
- **ConfigMaps:** Configuration management
- **Secrets:** Secure credential management
- **Network Policies:** Network security policies
- **Autoscaling:** HPA configurations

## 🏛️ Architecture Patterns

### Clean Architecture

All services follow Clean Architecture principles:

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│      (Controllers, DTOs)            │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Application Layer              │
│   (Use Cases, CQRS Bus)            │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Domain Layer                │
│   (Entities, Business Logic)        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Infrastructure Layer           │
│  (Database, Kafka, External APIs)   │
└─────────────────────────────────────┘
```

### CQRS Pattern

Commands and Queries are separated:

- **Commands:** Write operations (Create, Update, Delete)
- **Queries:** Read operations (Get, List, Search)
- **Events:** Domain events published to Kafka

### Event-Driven Architecture

- **Event Publishing:** Services publish domain events to Kafka
- **Event Consumption:** Services consume events asynchronously
- **Dead Letter Queue:** Failed events are routed to DLQ
- **Retry Mechanism:** Automatic retry with exponential backoff

## 🔒 Security

### Authentication

- **Custom JWT:** Self-contained JWT tokens
- **Keycloak:** OAuth2/OIDC integration
- **Dual Mode:** Support for both authentication methods

### Security Features

- Password encryption (BCrypt)
- Token validation
- CORS configuration
- Log masking for sensitive data
- Security headers
- Rate limiting (via API Gateway)

## 📚 Documentation

Comprehensive documentation is available in the `docs/` directory:

- **[Architecture Overview](docs/COMPLETE_ARCHITECTURE_OVERVIEW.md)** - Complete system architecture
- **[Microservices Demo Solution](docs/MICROSERVICES_DEMO_SOLUTION.md)** - Order & Payment services guide
- **[Kafka Wrapper Implementation](docs/KAFKA_WRAPPER_IMPLEMENTATION_SUMMARY.md)** - Kafka integration details
- **[Developer Quick Reference](docs/DEVELOPER_QUICK_REFERENCE.md)** - Quick start guide
- **[Code Quality Guide](docs/CODE_FORMATTING_AND_STYLE_GUIDE.md)** - Coding standards

### Service-Specific Documentation

- `auth-service/README.md` - Auth service documentation
- `backend-core/README.md` - Backend core library guide
- `ms-order-service/README.md` - Order service guide
- `ms-payment-service/README.md` - Payment service guide
- `ms-customer/README.md` - Customer service guide
- `monitoring/README.md` - Monitoring setup guide
- `eks/README.md` - Kubernetes deployment guide

## 🛠️ Development

### Code Quality

The project enforces code quality through:

- **Spotless:** Code formatting (Google Java Format)
- **Checkstyle:** Code style checking
- **Pre-commit Hooks:** Automated quality checks

```bash
# Format code
./gradlew spotlessApply

# Check code style
./gradlew checkstyleMain

# Run all quality checks
./gradlew codeQuality
```

### Project Structure

```
microservices-foundation/
├── api-gateway/          # API Gateway service
├── auth-service/         # Authentication service
├── backend-core/         # Shared foundation library
├── ms-customer/          # Customer service (reactive)
├── ms-order-service/     # Order service
├── ms-payment-service/   # Payment service
├── shared/               # Shared libraries
├── monitoring/           # Monitoring configurations
├── eks/                  # Kubernetes manifests
├── docs/                 # Documentation
├── scripts/              # Build and deployment scripts
└── test-integration/     # Integration tests
```

### Adding a New Service

1. Create service directory
2. Add `build.gradle` with backend-core dependency
3. Implement Clean Architecture layers
4. Configure application.yml
5. Add Dockerfile and docker-compose.yml
6. Create Kubernetes manifests in `eks/`
7. Add to build scripts

## 🔧 Configuration

### Environment Variables

Each service uses environment variables for configuration:

- Database connections
- Kafka brokers
- Redis endpoints
- Service URLs
- Security keys

See `env.template` in each service directory.

### Application Properties

Configuration files follow Spring Boot conventions:

- `application.yml` - Base configuration
- `application-dev.yml` - Development profile
- `application-prod.yml` - Production profile

## 📈 Performance

### Caching Strategy

- **L1 Cache (Caffeine):** Local in-memory cache
- **L2 Cache (Redis):** Distributed cache
- **Hibernate Cache:** Second-level cache for entities

### Database Optimization

- Connection pooling (HikariCP)
- Query optimization
- Index management
- Migration management (Liquibase)

## 🤝 Contributing

1. Follow the code style guide (`docs/CODE_FORMATTING_AND_STYLE_GUIDE.md`)
2. Run code quality checks before committing
3. Write tests for new features
4. Update documentation as needed
5. Follow Clean Architecture principles

## 📝 License

[Add your license information here]

## 🙏 Acknowledgments

Built with:
- Spring Boot 3.5
- Spring Cloud
- Apache Kafka
- PostgreSQL
- MongoDB
- Redis
- Keycloak
- Prometheus & Grafana
- Kubernetes

## 📞 Support

For issues and questions:
- Check the documentation in `docs/`
- Review service-specific README files
- Check integration test scripts for usage examples

---

**Last Updated:** 2025
**Version:** 1.0.0

