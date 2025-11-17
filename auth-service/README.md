# Auth-Service

Authentication and authorization microservice with support for Custom JWT and Keycloak authentication modes.

## 📁 Project Structure

```
auth-service/
├── scripts/                    # Shell scripts for different modes
│   ├── start-custom.sh        # Start with Custom JWT authentication
│   ├── start-keycloak.sh     # Start with Keycloak authentication
│   └── start-dual.sh         # Start with both authentication modes
├── document/                   # Documentation files
│   ├── README.md              # Project overview
│   ├── TESTING_GUIDE.md       # Comprehensive testing guide
│   └── QUICK_START.md         # Quick start reference
├── docker/                     # Docker configuration
│   ├── init-db/               # Database initialization scripts
│   └── docker-compose.yml     # Docker Compose configuration
└── src/                        # Source code
    └── main/java/com/demo/auth/
```

## 🚀 Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d postgres redis keycloak kafka
```

### 2. Start Auth-Service

```bash
# Custom JWT Mode
./scripts/start-custom.sh

# Keycloak Mode
./scripts/start-keycloak.sh

# Dual Mode (Both)
./scripts/start-dual.sh
```

### 3. Verify Services

```bash
curl http://localhost:8082/actuator/health
```

## 📚 Documentation

- **[Quick Start Guide](document/QUICK_START.md)** - Get started quickly
- **[Testing Guide](document/TESTING_GUIDE.md)** - Comprehensive testing instructions
- **[Project README](document/README.md)** - Detailed project overview

## 🔧 Features

- **Dual Authentication**: Custom JWT and Keycloak OAuth2
- **Multi-Layer Caching**: L1 (Caffeine) + L2 (Redis)
- **Log Masking**: Sensitive data protection
- **MapStruct**: Efficient object mapping
- **Database Migration**: Automated schema setup
- **Docker Support**: Complete containerized environment

## 🧪 Testing

See [TESTING_GUIDE.md](document/TESTING_GUIDE.md) for comprehensive testing scenarios including:

- Database migration testing
- Authentication mode testing (Custom JWT & Keycloak)
- Caching strategies and multi-layer caching
- Log masking verification
- Performance and security testing

## 🐳 Docker Services

- **PostgreSQL**: Database with auth schema
- **Redis**: Cache storage
- **Keycloak**: Identity provider
- **Kafka**: Message broker for event streaming
- **Zookeeper**: Kafka coordination service
- **Auth-Service**: Application service

## 📊 API Endpoints

- **Base URL**: `http://localhost:8082`
- **Documentation**: `http://localhost:8082/swagger-ui.html`
- **Health Check**: `http://localhost:8082/actuator/health`

## 🔐 Authentication Modes

### Custom JWT

- Username/password authentication
- Custom JWT token generation
- Refresh token support

### Keycloak

- OAuth2 authentication
- Keycloak JWT validation
- Single sign-on (SSO)

### Dual Mode

- Both authentication methods
- Automatic token type detection
- Seamless switching

## 📝 Scripts

| Script                      | Purpose                              |
| --------------------------- | ------------------------------------ |
| `scripts/start-custom.sh`   | Start with Custom JWT authentication |
| `scripts/start-keycloak.sh` | Start with Keycloak authentication   |
| `scripts/start-dual.sh`     | Start with both authentication modes |

## 🛠️ Development

### Prerequisites

- Java 21+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Keycloak 22+

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

## 📞 Support

For detailed information, see the documentation in the `document/` folder.
