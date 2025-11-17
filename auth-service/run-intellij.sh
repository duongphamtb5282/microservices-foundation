#!/bin/bash
cd /Users/duongphamthaibinh/Downloads/SourceCode/design/beautiful/java/microservices/auth-service

# Load environment variables properly
export DB_PASSWORD=auth_password
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8083
export LOG_LEVEL_AUTH=DEBUG
export AUTH_CACHE_TTL=10m
export KEYCLOAK_URL=http://localhost:8080
export KEYCLOAK_REALM=auth-service
export KEYCLOAK_CLIENT_ID=auth-service-client
export KEYCLOAK_CLIENT_SECRET=your-client-secret-here

echo "Environment variables set:"
echo "  DB_PASSWORD=$DB_PASSWORD"
echo "  SERVER_PORT=$SERVER_PORT"
echo "  SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE"

# Run the application
./gradlew bootRun
