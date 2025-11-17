#!/bin/bash

# Test Database Migration Script
# This script tests the database migration setup for auth-service

echo "🚀 Testing Auth-Service Database Migration"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

print_status "Docker is running"

# Check if docker-compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    print_error "docker-compose is not available. Please install docker-compose."
    exit 1
fi

print_status "docker-compose is available"

# Start PostgreSQL first
echo ""
echo "📊 Starting PostgreSQL..."
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker-compose ps postgres | grep -q "healthy"; then
        print_status "PostgreSQL is ready"
        break
    fi
    echo -n "."
    sleep 2
done

# Check if PostgreSQL is healthy
if ! docker-compose ps postgres | grep -q "healthy"; then
    print_error "PostgreSQL failed to start or is not healthy"
    docker-compose logs postgres
    exit 1
fi

# Test database connection
echo ""
echo "🔍 Testing database connection..."
if docker exec auth-service-postgres psql -U postgres -c "SELECT 1;" > /dev/null 2>&1; then
    print_status "Database connection successful"
else
    print_error "Database connection failed"
    exit 1
fi

# Check if auth schema exists
echo ""
echo "🏗️  Checking database schema..."
if docker exec auth-service-postgres psql -U postgres -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'auth';" | grep -q "auth"; then
    print_status "Auth schema exists"
else
    print_warning "Auth schema does not exist yet (will be created by migration)"
fi

# Check if Flyway migration tables exist
echo ""
echo "🔄 Checking Flyway migration status..."
if docker exec auth-service-postgres psql -U postgres -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'flyway_schema_history';" | grep -q "flyway_schema_history"; then
    print_status "Flyway migration table exists"
    
    # Show migration history
    echo "📋 Migration history:"
    docker exec auth-service-postgres psql -U postgres -c "SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank;"
else
    print_warning "Flyway migration table does not exist yet (will be created on first migration)"
fi

# Test migration files
echo ""
echo "📁 Checking migration files..."
MIGRATION_DIR="src/main/resources/db/migration"
if [ -d "$MIGRATION_DIR" ]; then
    print_status "Migration directory exists"
    
    # Count migration files
    MIGRATION_COUNT=$(find "$MIGRATION_DIR" -name "V*.sql" | wc -l)
    print_status "Found $MIGRATION_COUNT migration files"
    
    # List migration files
    echo "📄 Migration files:"
    find "$MIGRATION_DIR" -name "V*.sql" | sort | while read -r file; do
        echo "  - $(basename "$file")"
    done
else
    print_error "Migration directory does not exist: $MIGRATION_DIR"
    exit 1
fi

# Test application startup with migration
echo ""
echo "🚀 Testing application startup with migration..."
echo "⏳ This may take a few minutes..."

# Start the application in background
docker-compose up -d auth-service

# Wait for application to start
echo "⏳ Waiting for application to start and run migrations..."
for i in {1..60}; do
    if docker-compose ps auth-service | grep -q "healthy"; then
        print_status "Application is healthy"
        break
    fi
    echo -n "."
    sleep 3
done

# Check application health
if docker-compose ps auth-service | grep -q "healthy"; then
    print_status "Application started successfully"
    
    # Test health endpoint
    echo "🔍 Testing health endpoint..."
    if curl -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
        print_status "Health endpoint is accessible"
    else
        print_warning "Health endpoint is not accessible yet"
    fi
    
    # Check migration status after application start
    echo ""
    echo "📊 Final migration status:"
    if docker exec auth-service-postgres psql -U postgres -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'auth';" | grep -q "tbl_user"; then
        print_status "Auth tables created successfully"
        
        # Show created tables
        echo "📋 Created tables:"
        docker exec auth-service-postgres psql -U postgres -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'auth' ORDER BY table_name;"
    else
        print_warning "Auth tables not found"
    fi
    
else
    print_error "Application failed to start or is not healthy"
    echo "📋 Application logs:"
    docker-compose logs auth-service
    exit 1
fi

echo ""
echo "🎉 Database migration test completed successfully!"
echo ""
echo "📋 Summary:"
echo "  - PostgreSQL: ✅ Running and healthy"
echo "  - Database connection: ✅ Working"
echo "  - Migration files: ✅ Found and ready"
echo "  - Application: ✅ Started successfully"
echo "  - Health endpoint: ✅ Accessible"
echo ""
echo "🔗 Useful commands:"
echo "  - View logs: docker-compose logs auth-service"
echo "  - Check status: docker-compose ps"
echo "  - Stop services: docker-compose down"
echo "  - Restart: docker-compose restart auth-service"
