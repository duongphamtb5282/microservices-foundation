# Auth Service

The Auth Service handles all authentication and authorization functionality for the microservices ecosystem. It provides secure user management, token generation, and access control.

## Purpose

This service is responsible for user authentication, authorization, and session management across all microservices. It provides a centralized security layer that other services can rely on.

## Core Responsibilities

### 1. User Management

- **User Registration**: New user account creation
- **User Authentication**: Login and credential validation
- **User Profile**: User information management
- **Password Management**: Password hashing and validation

### 2. Authentication

- **Custom JWT**: Internal JWT token generation
- **Keycloak Integration**: External OAuth2/OIDC provider
- **Multi-Provider Support**: Seamless switching between auth providers
- **Token Validation**: JWT signature and expiration validation

### 3. Authorization

- **Role Management**: User role assignment and management
- **Permission Control**: Fine-grained access control
- **Resource Protection**: API endpoint authorization
- **Scope Management**: Token scope validation

### 4. Session Management

- **Stateless Sessions**: JWT-based session management
- **Refresh Tokens**: Long-lived token refresh mechanism
- **Token Revocation**: Secure token invalidation
- **Session Monitoring**: Active session tracking

## API Endpoints

### Authentication

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `POST /api/auth/refresh` - Token refresh

### User Management

- `GET /api/user/me` - Get current user profile
- `PUT /api/user/me` - Update user profile
- `POST /api/user/change-password` - Change password
- `GET /api/user/roles` - Get user roles

### Token Management

- `POST /api/token/validate` - Validate JWT token
- `POST /api/token/revoke` - Revoke JWT token
- `GET /api/token/info` - Get token information

### Admin Operations

- `GET /api/admin/users` - List all users
- `PUT /api/admin/users/{id}/roles` - Update user roles
- `DELETE /api/admin/users/{id}` - Delete user

## Configuration

### Application Properties

```yaml
spring:
  application:
    name: auth-service
  profiles:
    active: dev

auth:
  jwt:
    secret: ${JWT_SECRET:your-secret-key}
    expiration: 3600000 # 1 hour
    refresh-expiration: 604800000 # 7 days

  keycloak:
    enabled: true
    server-url: ${KEYCLOAK_URL:http://localhost:8080}
    realm: ${KEYCLOAK_REALM:master}
    client-id: ${KEYCLOAK_CLIENT_ID:api-java}

  security:
    password-strength: 8
    max-login-attempts: 5
    lockout-duration: 300000 # 5 minutes
```

## Database Schema

### User Tables

- `tbl_user` - User accounts
- `roles` - System roles
- `user_roles` - User-role assignments
- `refresh_tokens` - Refresh token storage

### Audit Tables

- `auth_audit_log` - Authentication events
- `token_audit_log` - Token usage tracking

## Security Features

### Password Security

- **BCrypt Hashing**: Secure password storage
- **Password Policies**: Configurable strength requirements
- **Account Lockout**: Brute force protection
- **Password History**: Prevent password reuse

### Token Security

- **RSA Signing**: Asymmetric key signing
- **Short Expiration**: Limited token lifetime
- **Refresh Mechanism**: Secure token renewal
- **Token Blacklisting**: Revocation support

### Access Control

- **Role-Based Access**: Hierarchical role system
- **Resource-Based Permissions**: Fine-grained control
- **API Rate Limiting**: Request throttling
- **CORS Protection**: Cross-origin security

## Service Dependencies

### Provides To

- **Comment Service**: User authentication and authorization
- **Foundation Service**: User data for audit trails
- **All Services**: JWT token validation

### Depends On

- **Foundation Service**: Database configuration, common utilities
- **PostgreSQL**: User and role data storage
- **Redis**: Session and token caching
- **Keycloak**: External authentication (optional)

## Authentication Flow

### 1. User Registration

```
Client → Auth Service → Password Hash → Database → JWT Token → Client
```

### 2. User Login

```
Client → Auth Service → Credential Validation → JWT Token → Client
```

### 3. Token Validation

```
Service → Auth Service → JWT Validation → User Info → Service
```

### 4. Keycloak Integration

```
Client → Keycloak → Auth Service → JWT Token → Client
```

## Multi-Provider Support

### Custom Authentication

- Username/password based
- JWT token generation
- Role-based authorization
- Database-backed user storage

### Keycloak Integration

- OAuth2/OIDC compliant
- External user management
- Federation support
- Advanced security features

### Provider Selection

- Header-based routing (`X-Auth-Provider`)
- Automatic fallback
- Seamless switching
- Configuration-driven

## Development

### Local Setup

1. Start dependencies:

   ```bash
   docker-compose up -d postgres redis keycloak
   ```

2. Run the service:

   ```bash
   ./mvnw spring-boot:run
   ```

3. Test authentication:

   ```bash
   # Register user
   curl -X POST http://localhost:8082/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

   # Login
   curl -X POST http://localhost:8082/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser","password":"password123"}'
   ```

### Testing

```bash
# Run all tests
./mvnw test

# Run security tests
./mvnw test -Dtest=*SecurityTest

# Run integration tests
./mvnw verify
```

## Monitoring

### Security Metrics

- Login success/failure rates
- Token generation/validation counts
- Password reset attempts
- Account lockout events

### Performance Metrics

- Authentication response times
- Token validation latency
- Database query performance
- Cache hit/miss ratios

### Audit Logging

- Authentication events
- Authorization decisions
- Token operations
- Administrative actions

## Deployment

### Docker

```dockerfile
FROM openjdk:21-jre-slim
COPY target/auth-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables

```bash
JWT_SECRET=your-secret-key
KEYCLOAK_URL=http://keycloak:8080
KEYCLOAK_REALM=master
KEYCLOAK_CLIENT_ID=api-java
DATABASE_URL=jdbc:postgresql://postgres:5432/auth_db
REDIS_URL=redis://redis:6379
```

## Security Considerations

### Data Protection

- **Encryption at Rest**: Database encryption
- **Encryption in Transit**: TLS/SSL
- **Sensitive Data Masking**: Log protection
- **PII Handling**: GDPR compliance

### Access Control

- **Least Privilege**: Minimal required permissions
- **Principle of Separation**: Isolated responsibilities
- **Defense in Depth**: Multiple security layers
- **Regular Audits**: Security reviews

### Compliance

- **GDPR**: Data protection compliance
- **SOC 2**: Security controls
- **OWASP**: Security best practices
- **PCI DSS**: Payment card security (if applicable)

## Future Enhancements

1. **Multi-Factor Authentication**: TOTP/SMS support
2. **Social Login**: OAuth2 providers (Google, GitHub)
3. **Biometric Authentication**: Fingerprint/face recognition
4. **Advanced Threat Detection**: Anomaly detection
5. **Federation**: SAML integration
6. **Zero Trust Architecture**: Continuous verification
