#!/bin/bash

# Quick Cache Test Script
# Simple tests for user cache functionality

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Configuration
AUTH_SERVICE_URL="http://localhost:8082"
BASE_URL="$AUTH_SERVICE_URL/api/v1"

# Test credentials
USERNAME="cacheuser"
PASSWORD="password123"

# JWT token
JWT_TOKEN=""

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

# Check if service is running
check_service() {
    log_info "Checking auth-service..."
    if curl -s "$AUTH_SERVICE_URL/actuator/health" > /dev/null; then
        log_success "Service is running"
        return 0
    else
        log_error "Service is not running"
        return 1
    fi
}

# Login and get token
login() {
    log_info "Logging in..."
    local response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")
    
    JWT_TOKEN=$(echo "$response" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$JWT_TOKEN" ]; then
        log_success "Login successful"
        return 0
    else
        log_error "Login failed: $response"
        return 1
    fi
}

# Test cache performance
test_cache_performance() {
    log_info "Testing cache performance..."
    
    # First request (should hit database)
    log_info "First request (database hit)..."
    local start1=$(date +%s%3N)
    local response1=$(curl -s -X GET "$BASE_URL/users/info" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local end1=$(date +%s%3N)
    local time1=$((end1 - start1))
    
    echo "First request: ${time1}ms"
    echo "Response: $(echo "$response1" | jq '.userName' 2>/dev/null || echo 'N/A')"
    
    # Second request (should hit cache)
    log_info "Second request (cache hit)..."
    local start2=$(date +%s%3N)
    local response2=$(curl -s -X GET "$BASE_URL/users/info" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local end2=$(date +%s%3N)
    local time2=$((end2 - start2))
    
    echo "Second request: ${time2}ms"
    echo "Response: $(echo "$response2" | jq '.userName' 2>/dev/null || echo 'N/A')"
    
    # Compare performance
    if [ $time2 -lt $time1 ]; then
        log_success "Cache is working! (${time2}ms vs ${time1}ms)"
    else
        log_warning "Cache might not be working optimally"
    fi
}

# Test cache statistics
test_cache_stats() {
    log_info "Testing cache statistics..."
    
    local response=$(curl -s -X GET "$BASE_URL/cache/stats" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Cache Statistics:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# Test cache clear
test_cache_clear() {
    log_info "Testing cache clear..."
    
    local response=$(curl -s -X POST "$BASE_URL/users/cache/clear" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Cache Clear Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# Main test
main() {
    echo "=========================================="
    echo "Quick Cache Test"
    echo "=========================================="
    
    if ! check_service; then
        exit 1
    fi
    
    if ! login; then
        exit 1
    fi
    
    echo "=========================================="
    test_cache_performance
    echo "=========================================="
    test_cache_stats
    echo "=========================================="
    test_cache_clear
    echo "=========================================="
    
    log_success "Quick cache test completed!"
}

# Run main function
main "$@"
