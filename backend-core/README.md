# Foundation Service

The Foundation Service provides core infrastructure and shared services for the microservices ecosystem. It serves as the backbone that other services depend on for common functionality.

## Purpose

This service encapsulates all the foundational components that are shared across multiple microservices, reducing code duplication and ensuring consistency.

## Core Responsibilities

### 1. Database Infrastructure

- **Connection Pooling**: HikariCP configuration
- **Database Configuration**: Multi-tenant database setup
- **Migration Management**: Liquibase changelog management
- **Audit Support**: Base entity classes with audit fields

### 2. Caching Layer

- **L1 Cache**: Caffeine local cache
- **L2 Cache**: Redis distributed cache
- **Cache Configuration**: TTL, eviction policies
- **Cache Monitoring**: Cache hit/miss metrics

### 3. Security Foundation

- **Security Configuration Templates**: Base security setup
- **JWT Utilities**: Token validation helpers
- **Password Encoding**: BCrypt configuration
- **CORS Configuration**: Cross-origin resource sharing

### 4. Logging and Monitoring

- **Logback Configuration**: Structured logging setup
- **Log Masking**: Sensitive data protection
- **Metrics Collection**: Micrometer integration
- **Health Checks**: Service health monitoring

### 5. Common Utilities

- **Date/Time Utilities**: Timezone handling
- **Validation Helpers**: Common validation logic
- **Serialization**: JSON serialization configuration
- **Exception Handling**: Global exception handling

## API Endpoints

### Health and Monitoring

- `GET /actuator/health` - Service health status
- `GET /actuator/info` - Service information
- `GET /actuator/metrics` - Service metrics

### Cache Management

- `GET /api/cache/test/caffeine` - Test Caffeine cache
- `GET /api/cache/test/redis` - Test Redis cache
- `GET /api/cache/test/hibernate` - Test Hibernate cache

### Utility Services

- `GET /api/utils/health` - Utility health check
- `POST /api/utils/validate` - Generic validation endpoint

## Configuration

### Application Properties

```yaml
spring:
  application:
    name: foundation-service
  profiles:
    active: dev

foundation:
  cache:
    caffeine:
      maximum-size: 1000
      expire-after-write: 10m
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

  database:
    pool:
      minimum-idle: 2
      maximum-pool-size: 10
      connection-timeout: 20000ms
```

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Cache
- Spring Boot Starter Actuator

### Database Dependencies

- PostgreSQL Driver
- HikariCP
- Liquibase

### Cache Dependencies

- Caffeine
- Spring Data Redis
- Lettuce

### Monitoring Dependencies

- Micrometer
- Micrometer Registry Prometheus

## Database Schema

### Base Tables

- `audit_log` - System audit trail
- `system_config` - Configuration management
- `cache_metadata` - Cache management

### Base Entity Classes

- `BaseUuidEntity` - UUID-based entities with audit
- `BaseLongEntity` - Long ID-based entities with audit
- `AuditFields` - Common audit field interface

## Service Dependencies

### Provides To

- **Auth Service**: Database config, security templates, JWT utilities
- **Comment Service**: Database config, caching, common utilities

### Depends On

- **PostgreSQL**: Primary database
- **Redis**: Cache and session storage

## Development

### Local Setup

1. Start dependencies:

   ```bash
   docker-compose up -d postgres redis
   ```

2. Run the service:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Verify health:
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Testing

```bash
# Run all tests
./mvnw test

# Run specific test category
./mvnw test -Dtest=*CacheTest

# Run integration tests
./mvnw verify
```

## Monitoring

### Health Checks

- Database connectivity
- Redis connectivity
- Cache performance
- Memory usage

### Metrics

- Cache hit/miss ratios
- Database connection pool status
- Request/response times
- Error rates

## Deployment

### Docker

```dockerfile
FROM openjdk:21-jre-slim
COPY target/foundation-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose

```yaml
foundation-service:
  build: ./foundation-service
  ports:
    - "8081:8081"
  environment:
    - SPRING_PROFILES_ACTIVE=prod
  depends_on:
    - postgres
    - redis
```

## Future Enhancements

1. **Service Discovery**: Integration with Eureka/Consul
2. **Configuration Management**: Spring Cloud Config
3. **Circuit Breaker**: Resilience4j integration
4. **Distributed Tracing**: Zipkin integration
5. **Message Queues**: RabbitMQ/Kafka integration
