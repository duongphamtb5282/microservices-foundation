#!/bin/bash

# Start Auth Service in Keycloak Mode
echo "🚀 Starting Auth Service in Keycloak Mode..."

# Set the profile to keycloak
export SPRING_PROFILES_ACTIVE=keycloak

# Start the application
./gradlew :auth-service:bootRun --args="--spring.profiles.active=keycloak"

echo "✅ Auth Service started in Keycloak Mode"
echo "📡 Service will be available at: http://localhost:8082"
echo "🔐 Authentication: Keycloak OAuth2"
echo "📚 API Documentation: http://localhost:8082/swagger-ui.html"
echo "⚠️  Make sure Keycloak is running at: http://localhost:8080"
