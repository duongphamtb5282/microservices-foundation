#!/bin/bash

# Start Auth Service in Dual Mode
echo "🚀 Starting Auth Service in Dual Mode..."

# Set the profile to dual
export SPRING_PROFILES_ACTIVE=dual

# Start the application
./gradlew :auth-service:bootRun --args="--spring.profiles.active=dual"

echo "✅ Auth Service started in Dual Mode"
echo "📡 Service will be available at: http://localhost:8082"
echo "🔐 Authentication: Both Custom JWT and Keycloak OAuth2"
echo "📚 API Documentation: http://localhost:8082/swagger-ui.html"
echo "⚠️  Make sure Keycloak is running at: http://localhost:8080"
