#!/bin/bash

# Simple Cache Test Script for Auth Service
# Tests basic functionality and caching

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
AUTH_SERVICE_URL="http://localhost:8082"
BASE_URL="$AUTH_SERVICE_URL/api"

# Test user credentials
TEST_USERNAME="duong"
TEST_PASSWORD="password123"
TEST_EMAIL="duong@example.com"

# JWT token storage
JWT_TOKEN=""

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_test() {
    echo -e "${PURPLE}[TEST]${NC} $1"
}

# Check if auth-service is running
check_service() {
    log_info "Checking if auth-service is running..."
    if curl -s "$AUTH_SERVICE_URL/actuator/health" > /dev/null; then
        log_success "Auth-service is running"
        return 0
    else
        log_error "Auth-service is not running. Please start it first."
        return 1
    fi
}

# Register a test user
register_user() {
    log_test "Registering test user..."
    
    local response=$(curl -s -X POST "$BASE_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d "{
            \"userName\": \"$TEST_USERNAME\",
            \"email\": \"$TEST_EMAIL\",
            \"password\": \"$TEST_PASSWORD\"
        }")
    
    if echo "$response" | grep -q "successfully"; then
        log_success "User registered successfully"
        return 0
    else
        log_warning "User might already exist or registration failed: $response"
        return 1
    fi
}

# Login and get JWT token
login_user() {
    log_test "Logging in user to get JWT token..."
    
    local response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$TEST_USERNAME\",
            \"password\": \"$TEST_PASSWORD\"
        }")
    
    JWT_TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$JWT_TOKEN" ]; then
        log_success "Login successful, JWT token obtained"
        return 0
    else
        log_error "Login failed: $response"
        return 1
    fi
}

# Test user info endpoint
test_user_info() {
    log_test "Testing user info endpoint..."
    
    if [ -z "$JWT_TOKEN" ]; then
        log_error "No JWT token available"
        return 1
    fi
    
    log_info "First request (should hit database)..."
    local start_time=$(date +%s)
    local response1=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local end_time=$(date +%s)
    local duration1=$((end_time - start_time))
    
    echo "First request took: ${duration1}s"
    echo "Response: $(echo "$response1" | jq '.username' 2>/dev/null || echo 'N/A')"
    
    log_info "Second request (should hit cache)..."
    start_time=$(date +%s)
    local response2=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    end_time=$(date +%s)
    local duration2=$((end_time - start_time))
    
    echo "Second request took: ${duration2}s"
    echo "Response: $(echo "$response2" | jq '.username' 2>/dev/null || echo 'N/A')"
    
    if [ $duration2 -lt $duration1 ]; then
        log_success "Cache is working! Second request was faster (${duration2}s vs ${duration1}s)"
    else
        log_warning "Cache might not be working optimally"
    fi
    echo
}

# Test application health
test_health() {
    log_test "Testing application health..."
    
    local response=$(curl -s "$AUTH_SERVICE_URL/actuator/health")
    echo "Health Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test actuator endpoints
test_actuator() {
    log_test "Testing actuator endpoints..."
    
    log_info "Testing /actuator/info"
    curl -s "$AUTH_SERVICE_URL/actuator/info" | jq '.' 2>/dev/null || echo "No info available"
    
    log_info "Testing /actuator/metrics"
    curl -s "$AUTH_SERVICE_URL/actuator/metrics" | jq '.names' 2>/dev/null || echo "No metrics available"
    
    echo
}

# Main test function
run_tests() {
    log_info "Starting Simple Cache Tests..."
    echo "=========================================="
    
    # Check service
    if ! check_service; then
        exit 1
    fi
    
    # Test health
    test_health
    
    # Test actuator
    test_actuator
    
    # Register and login
    register_user
    if ! login_user; then
        log_error "Failed to login. Cannot proceed with user tests."
        exit 1
    fi
    
    # Test user info caching
    test_user_info
    
    echo "=========================================="
    log_success "All tests completed!"
    echo "=========================================="
}

# Run the tests
run_tests
