#!/bin/bash

# Start Auth Service in Custom JWT Mode
echo "🚀 Starting Auth Service in Custom JWT Mode..."

# Set the profile to custom
export SPRING_PROFILES_ACTIVE=dev

# Start the application
./gradlew :auth-service:bootRun --args="--spring.profiles.active=dev"

echo "✅ Auth Service started in Custom JWT Mode"
echo "📡 Service will be available at: http://localhost:8082"
echo "🔐 Authentication: Custom JWT"
echo "📚 API Documentation: http://localhost:8082/swagger-ui.html"
