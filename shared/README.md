# Shared Libraries

This directory contains shared libraries and common code used across all microservices. It promotes code reuse, consistency, and reduces duplication.

## Purpose

The shared libraries provide common functionality that multiple microservices need, ensuring consistency and reducing maintenance overhead.

## Library Structure

```
shared/
├── common/           # Common data structures and utilities
├── config/           # Shared configuration classes
├── exceptions/       # Common exception handling
├── utils/            # Utility functions and helpers
└── README.md        # This file
```

## Common Libraries

### 1. Common (`shared/common/`)

#### DTOs (Data Transfer Objects)

- **BaseDTO**: Common fields for all DTOs
- **PageDTO**: Pagination response wrapper
- **ErrorDTO**: Standardized error response
- **ValidationDTO**: Validation error details

#### Enums

- **StatusEnum**: Common status values
- **PriorityEnum**: Priority levels
- **ContentTypeEnum**: Content type definitions

#### Constants

- **ApiConstants**: API endpoint constants
- **ErrorConstants**: Error code constants
- **ValidationConstants**: Validation rules

### 2. Configuration (`shared/config/`)

#### Database Configuration

- **DatabaseConfig**: Common database setup
- **JpaConfig**: JPA configuration
- **LiquibaseConfig**: Database migration setup

#### Security Configuration

- **SecurityConfig**: Base security configuration
- **JwtConfig**: JWT utilities and configuration
- **CorsConfig**: CORS configuration

#### Cache Configuration

- **CacheConfig**: Cache configuration templates
- **RedisConfig**: Redis connection setup
- **CaffeineConfig**: Local cache configuration

#### Monitoring Configuration

- **ActuatorConfig**: Health check configuration
- **MetricsConfig**: Metrics collection setup
- **LoggingConfig**: Logging configuration

### 3. Exceptions (`shared/exceptions/`)

#### Base Exceptions

- **BaseException**: Common exception base class
- **BusinessException**: Business logic exceptions
- **ValidationException**: Input validation exceptions
- **AuthenticationException**: Authentication failures
- **AuthorizationException**: Authorization failures

#### Exception Handlers

- **GlobalExceptionHandler**: Centralized exception handling
- **ValidationExceptionHandler**: Validation error handling
- **SecurityExceptionHandler**: Security error handling

#### Error Response DTOs

- **ErrorResponse**: Standardized error response
- **FieldError**: Field-specific validation errors
- **ErrorDetail**: Detailed error information

### 4. Utils (`shared/utils/`)

#### Date/Time Utilities

- **DateUtils**: Date manipulation helpers
- **TimeUtils**: Time calculation utilities
- **TimezoneUtils**: Timezone handling

#### String Utilities

- **StringUtils**: String manipulation helpers
- **ValidationUtils**: Input validation utilities
- **SanitizationUtils**: Data sanitization

#### Security Utilities

- **PasswordUtils**: Password handling
- **TokenUtils**: JWT token utilities
- **EncryptionUtils**: Data encryption helpers

#### Serialization Utilities

- **JsonUtils**: JSON serialization helpers
- **XmlUtils**: XML serialization utilities
- **CsvUtils**: CSV processing utilities

## Usage in Microservices

### Maven Dependency

```xml
<dependency>
    <groupId>com.pacific</groupId>
    <artifactId>shared-libraries</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'com.pacific:shared-libraries:1.0.0'
```

### Import Examples

```java
// Common DTOs
import com.pacific.shared.common.dto.BaseDTO;
import com.pacific.shared.common.dto.PageDTO;

// Exceptions
import com.pacific.shared.exceptions.BusinessException;
import com.pacific.shared.exceptions.ValidationException;

// Utilities
import com.pacific.shared.utils.DateUtils;
import com.pacific.shared.utils.StringUtils;

// Configuration
import com.pacific.shared.config.DatabaseConfig;
import com.pacific.shared.config.SecurityConfig;
```

## Configuration Management

### Application Properties

```yaml
shared:
  database:
    default-pool-size: 10
    connection-timeout: 20000ms

  cache:
    default-ttl: 3600s
    max-size: 1000

  security:
    jwt:
      default-expiration: 3600000
      refresh-expiration: 604800000

  validation:
    email-pattern: "^[A-Za-z0-9+_.-]+@(.+)$"
    password-min-length: 8
```

### Environment-Specific Configuration

```yaml
# application-dev.yml
shared:
  database:
    url: jdbc:postgresql://localhost:5432/dev_db

  cache:
    redis:
      host: localhost
      port: 6379

# application-prod.yml
shared:
  database:
    url: jdbc:postgresql://prod-db:5432/prod_db

  cache:
    redis:
      host: prod-redis
      port: 6379
```

## Versioning Strategy

### Semantic Versioning

- **Major Version**: Breaking changes
- **Minor Version**: New features (backward compatible)
- **Patch Version**: Bug fixes (backward compatible)

### Version Compatibility

- **Foundation Service**: Always uses latest shared libraries
- **Auth Service**: Compatible with shared libraries v1.x
- **Comment Service**: Compatible with shared libraries v1.x

### Migration Guide

When upgrading shared libraries:

1. Check breaking changes in release notes
2. Update import statements if needed
3. Test all dependent services
4. Update configuration if required

## Testing

### Unit Tests

```bash
# Run all shared library tests
./mvnw test

# Run specific test category
./mvnw test -Dtest=*UtilsTest
./mvnw test -Dtest=*ExceptionTest
```

### Integration Tests

```bash
# Run integration tests
./mvnw verify

# Test with different configurations
./mvnw test -Dspring.profiles.active=test
```

## Documentation

### API Documentation

- **Swagger/OpenAPI**: Auto-generated API docs
- **JavaDoc**: Comprehensive code documentation
- **Usage Examples**: Code samples and tutorials

### Migration Guides

- **Version Upgrades**: Step-by-step upgrade instructions
- **Breaking Changes**: Detailed change descriptions
- **Best Practices**: Recommended usage patterns

## Deployment

### Maven Repository

```xml
<repositories>
    <repository>
        <id>shared-libs-repo</id>
        <url>https://nexus.company.com/repository/maven-public/</url>
    </repository>
</repositories>
```

### Docker Base Image

```dockerfile
FROM openjdk:21-jre-slim
COPY shared-libraries.jar /app/libs/
# Services can reference shared libraries from /app/libs/
```

## Development Setup

### IntelliJ IDEA Integration

#### Code Quality Tools
This module uses automated code quality tools that integrate seamlessly with IntelliJ IDEA:

- **Spotless**: Code formatting and import organization
- **Checkstyle**: Code style validation
- **Google Java Style**: Consistent formatting standards

#### Required Plugins
Install these IntelliJ plugins for full integration:
- **CheckStyle-IDEA**: For real-time style checking
- **Spotless Gradle** (optional): For enhanced formatting integration

#### Quick Setup
1. Open the project in IntelliJ IDEA
2. Install required plugins (CheckStyle-IDEA recommended)
3. Code style will be automatically applied via project settings

#### Manual Formatting
```bash
# Format code and organize imports
./gradlew spotlessApply

# Check code style compliance
./gradlew checkstyleMain

# Run all quality checks
./gradlew preCommit
```

#### IntelliJ Configuration
The project includes `.idea` configuration files that automatically:
- Set Google Java Style as the default code style
- Configure import organization rules
- Enable code quality tool integration

#### Keyboard Shortcuts (Recommended)
- `Ctrl+Alt+L`: Reformat code
- `Ctrl+Alt+O`: Optimize imports
- `Ctrl+Alt+F`: Run Spotless format (external tool)
- `Ctrl+Alt+C`: Run Checkstyle check (external tool)

### Build System

#### Gradle Tasks
```bash
# Code Quality
./gradlew spotlessCheck      # Check formatting
./gradlew checkstyleMain     # Check style violations
./gradlew preCommit          # Pre-commit validation

# Development
./gradlew clean build        # Full build
./gradlew test              # Run tests
./gradlew publish           # Publish to repository
```

## Best Practices

### Code Organization

- **Single Responsibility**: Each utility class has one purpose
- **Immutable Objects**: DTOs and value objects are immutable
- **Null Safety**: Proper null handling throughout
- **Thread Safety**: Utilities are thread-safe

### Performance

- **Lazy Loading**: Load resources only when needed
- **Caching**: Cache expensive operations
- **Connection Pooling**: Reuse database connections
- **Memory Management**: Efficient memory usage

### Security

- **Input Validation**: Validate all inputs
- **Output Encoding**: Encode outputs properly
- **Secret Management**: Never hardcode secrets
- **Audit Logging**: Log security-relevant events

## Future Enhancements

1. **Service Discovery**: Integration with service registry
2. **Circuit Breaker**: Resilience patterns
3. **Distributed Tracing**: Request tracing support
4. **Metrics Collection**: Standardized metrics
5. **Configuration Management**: External configuration
6. **Message Queues**: Event-driven communication
7. **API Gateway**: Request routing and filtering
8. **Monitoring**: Health checks and alerting
