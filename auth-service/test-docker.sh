#!/bin/bash

# Auth Service Docker Testing Script
# This script helps you test the auth-service with Docker infrastructure

set -e

echo "🚀 Starting Auth Service Docker Infrastructure..."
echo "================================================="

# Start Docker infrastructure (without auth-service)
echo "📦 Starting PostgreSQL, Redis, Kafka, and Keycloak..."
docker-compose up -d postgres redis zookeeper kafka keycloak

echo "⏳ Waiting for services to be healthy..."
echo "   - PostgreSQL: Waiting for database to be ready..."
sleep 10

# Wait for PostgreSQL
echo "   - Checking PostgreSQL health..."
until docker-compose exec -T postgres pg_isready -U auth_user -d auth_service; do
  echo "   - PostgreSQL not ready, waiting..."
  sleep 5
done

echo "   - Checking Redis health..."
until docker-compose exec -T redis redis-cli ping | grep -q PONG; do
  echo "   - Redis not ready, waiting..."
  sleep 3
done

echo "   - Checking Kafka health..."
until docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  echo "   - Kafka not ready, waiting..."
  sleep 5
done

echo "   - Checking Keycloak health..."
until curl -f http://localhost:8080/health/ready > /dev/null 2>&1; do
  echo "   - Keycloak not ready, waiting..."
  sleep 10
done

echo "✅ All infrastructure services are ready!"
echo ""
echo "🌐 Service URLs:"
echo "   - PostgreSQL: localhost:5432 (auth_service database)"
echo "   - Redis: localhost:6379"
echo "   - Kafka: localhost:9092"
echo "   - Keycloak: http://localhost:8080"
echo ""
echo "🔧 Now start the auth-service manually:"
echo "   cd auth-service"
echo "   ./gradlew bootRun --args='--spring.profiles.active=dev'"
echo ""
echo "🧪 Test the API:"
echo "   curl http://localhost:8082/actuator/health"
echo "   curl http://localhost:8082/v3/api-docs"
echo ""
echo "🛑 To stop infrastructure:"
echo "   docker-compose down"
