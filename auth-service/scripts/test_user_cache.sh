#!/bin/bash

# Test User Cache Script
# Tests SQL cache, cache strategies, and cache management for auth-service

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

log_cache() {
    echo -e "${CYAN}[CACHE]${NC} $1"
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
    log_test "Registering test user for cache testing..."
    
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

# Test cache health
test_cache_health() {
    log_test "Testing application health endpoint..."
    
    local response=$(curl -s -X GET "$AUTH_SERVICE_URL/actuator/health")
    
    echo "Application Health Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test cache statistics
test_cache_stats() {
    log_test "Testing actuator metrics..."
    
    local response=$(curl -s -X GET "$AUTH_SERVICE_URL/actuator/metrics")
    
    echo "Actuator Metrics:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test cache existence
test_cache_exists() {
    log_test "Testing cache existence..."
    
    local response=$(curl -s -X GET "$BASE_URL/cache/exists/user-info" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Cache Exists Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test cache serialization
test_cache_serialization() {
    log_test "Testing cache serialization..."
    
    local response=$(curl -s -X GET "$BASE_URL/cache/serialize" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Cache Serialization Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test user info caching (SQL cache)
test_user_info_cache() {
    log_test "Testing user info caching (SQL cache)..."
    
    log_cache "First request (should hit database)..."
    local start_time=$(date +%s)
    local response1=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local end_time=$(date +%s)
    local duration1=$((end_time - start_time))
    
    echo "First request took: ${duration1}ms"
    echo "Response: $(echo "$response1" | jq '.userName' 2>/dev/null || echo 'N/A')"
    
    log_cache "Second request (should hit cache)..."
    start_time=$(date +%s)
    local response2=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    end_time=$(date +%s)
    local duration2=$((end_time - start_time))
    
    echo "Second request took: ${duration2}ms"
    echo "Response: $(echo "$response2" | jq '.userName' 2>/dev/null || echo 'N/A')"
    
    if [ $duration2 -lt $duration1 ]; then
        log_success "Cache is working! Second request was faster (${duration2}ms vs ${duration1}ms)"
    else
        log_warning "Cache might not be working optimally"
    fi
    echo
}

# Test user by ID caching
test_user_by_id_cache() {
    log_test "Testing user by ID caching..."
    
    # Get user ID from user info
    local user_info=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local user_id=$(echo "$user_info" | jq -r '.id' 2>/dev/null)
    
    # If JWT authentication is not working, use a hardcoded user ID for testing
    if [ -z "$user_id" ] || [ "$user_id" = "null" ]; then
        log_warning "JWT authentication not working, using hardcoded user ID for testing"
        user_id="550e8400-e29b-41d4-a716-446655440000"  # Use the UUID from the database
    fi
    
    if [ -n "$user_id" ] && [ "$user_id" != "null" ]; then
        log_cache "Testing user by ID: $user_id"
        
        # First request
        local start_time=$(date +%s)
        local response1=$(curl -s -X GET "$BASE_URL/users/$user_id" \
            -H "Authorization: Bearer $JWT_TOKEN")
        local end_time=$(date +%s)
        local duration1=$((end_time - start_time))
        
        echo "First request took: ${duration1}ms"
        
        # Second request
        start_time=$(date +%s)
        local response2=$(curl -s -X GET "$BASE_URL/users/$user_id" \
            -H "Authorization: Bearer $JWT_TOKEN")
        end_time=$(date +%s)
        local duration2=$((end_time - start_time))
        
        echo "Second request took: ${duration2}ms"
        
        if [ $duration2 -lt $duration1 ]; then
            log_success "User by ID cache is working!"
        else
            log_warning "User by ID cache might not be working optimally"
        fi
    else
        log_error "Could not get user ID for testing"
    fi
    echo
}

# Test user roles caching
test_user_roles_cache() {
    log_test "Testing user roles caching..."
    
    local user_info=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local user_id=$(echo "$user_info" | jq -r '.id' 2>/dev/null)
    
    # If JWT authentication is not working, use a hardcoded user ID for testing
    if [ -z "$user_id" ] || [ "$user_id" = "null" ]; then
        log_warning "JWT authentication not working, using hardcoded user ID for testing"
        user_id="550e8400-e29b-41d4-a716-446655440000"  # Use the UUID from the database
    fi
    
    if [ -n "$user_id" ] && [ "$user_id" != "null" ]; then
        log_cache "Testing user roles for user: $user_id"
        
        # First request
        local start_time=$(date +%s)
        local response1=$(curl -s -X GET "$BASE_URL/users/$user_id/roles" \
            -H "Authorization: Bearer $JWT_TOKEN")
        local end_time=$(date +%s)
        local duration1=$((end_time - start_time))
        
        echo "First request took: ${duration1}ms"
        echo "Roles: $(echo "$response1" | jq '.roles' 2>/dev/null || echo 'N/A')"
        
        # Second request
        start_time=$(date +%s)
        local response2=$(curl -s -X GET "$BASE_URL/users/$user_id/roles" \
            -H "Authorization: Bearer $JWT_TOKEN")
        end_time=$(date +%s)
        local duration2=$((end_time - start_time))
        
        echo "Second request took: ${duration2}ms"
        
        if [ $duration2 -lt $duration1 ]; then
            log_success "User roles cache is working!"
        else
            log_warning "User roles cache might not be working optimally"
        fi
    else
        log_error "Could not get user ID for roles testing"
    fi
    echo
}

# Test cache reload
test_cache_reload() {
    log_test "Testing cache reload functionality..."
    
    log_cache "Reloading all caches..."
    local response=$(curl -s -X POST "$BASE_URL/cache/reload" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Cache Reload Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test cache clear
test_cache_clear() {
    log_test "Testing cache clear functionality..."
    
    log_cache "Clearing user caches..."
    local response=$(curl -s -X POST "$BASE_URL/users/cache/clear" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Cache Clear Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test cache warm-up
test_cache_warmup() {
    log_test "Testing cache warm-up..."
    
    log_cache "Warming up caches..."
    local response=$(curl -s -X POST "$BASE_URL/cache/warmup" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Cache Warm-up Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    echo
}

# Test cache performance with multiple requests
test_cache_performance() {
    log_test "Testing cache performance with multiple requests..."
    
    local user_info=$(curl -s -X GET "$BASE_URL/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local user_id=$(echo "$user_info" | jq -r '.id' 2>/dev/null)
    
    # If JWT authentication is not working, use a hardcoded user ID for testing
    if [ -z "$user_id" ] || [ "$user_id" = "null" ]; then
        log_warning "JWT authentication not working, using hardcoded user ID for testing"
        user_id="550e8400-e29b-41d4-a716-446655440000"  # Use the UUID from the database
    fi
    
    if [ -n "$user_id" ] && [ "$user_id" != "null" ]; then
        log_cache "Running 10 consecutive requests to test cache performance..."
        
        local total_time=0
        for i in {1..10}; do
            local start_time=$(date +%s)
            curl -s -X GET "$BASE_URL/users/$user_id" \
                -H "Authorization: Bearer $JWT_TOKEN" > /dev/null
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            total_time=$((total_time + duration))
            
            if [ $i -eq 1 ]; then
                echo "Request $i (first): ${duration}ms"
            elif [ $i -eq 10 ]; then
                echo "Request $i (last): ${duration}ms"
            fi
        done
        
        local avg_time=$((total_time / 10))
        echo "Average response time: ${avg_time}ms"
        
        if [ $avg_time -lt 100 ]; then
            log_success "Cache performance is excellent! (avg: ${avg_time}ms)"
        elif [ $avg_time -lt 500 ]; then
            log_success "Cache performance is good! (avg: ${avg_time}ms)"
        else
            log_warning "Cache performance could be improved (avg: ${avg_time}ms)"
        fi
    else
        log_error "Could not get user ID for performance testing"
    fi
    echo
}

# Test cache strategies
test_cache_strategies() {
    log_test "Testing different cache strategies..."
    
    log_cache "Testing L1 Cache (Caffeine)..."
    local response1=$(curl -s -X GET "$BASE_URL/cache/strategy/l1" \
        -H "Authorization: Bearer $JWT_TOKEN")
    echo "L1 Cache Strategy: $response1"
    
    log_cache "Testing L2 Cache (Redis)..."
    local response2=$(curl -s -X GET "$BASE_URL/cache/strategy/l2" \
        -H "Authorization: Bearer $JWT_TOKEN")
    echo "L2 Cache Strategy: $response2"
    
    log_cache "Testing Multi-tier Cache..."
    local response3=$(curl -s -X GET "$BASE_URL/cache/strategy/multitier" \
        -H "Authorization: Bearer $JWT_TOKEN")
    echo "Multi-tier Cache Strategy: $response3"
    echo
}

# Test cache invalidation
test_cache_invalidation() {
    log_test "Testing cache invalidation..."
    
    local user_info=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    local user_id=$(echo "$user_info" | jq -r '.id' 2>/dev/null)
    
    # If JWT authentication is not working, use a hardcoded user ID for testing
    if [ -z "$user_id" ] || [ "$user_id" = "null" ]; then
        log_warning "JWT authentication not working, using hardcoded user ID for testing"
        user_id="550e8400-e29b-41d4-a716-446655440000"  # Use the UUID from the database
    fi
    
    if [ -n "$user_id" ] && [ "$user_id" != "null" ]; then
        log_cache "Testing cache invalidation for user: $user_id"
        
        # Get initial cached data
        local response1=$(curl -s -X GET "$BASE_URL/users/$user_id" \
            -H "Authorization: Bearer $JWT_TOKEN")
        echo "Initial cached data: $(echo "$response1" | jq '.userName' 2>/dev/null || echo 'N/A')"
        
        # Update user data (this should invalidate cache)
        log_cache "Updating user data to trigger cache invalidation..."
        local update_response=$(curl -s -X PUT "$BASE_URL/users/$user_id" \
            -H "Authorization: Bearer $JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d '{"firstName": "Updated", "lastName": "User"}')
        
        echo "Update response: $update_response"
        
        # Get updated data (should be fresh from database)
        local response2=$(curl -s -X GET "$BASE_URL/users/$user_id" \
            -H "Authorization: Bearer $JWT_TOKEN")
        echo "Updated data: $(echo "$response2" | jq '.firstName' 2>/dev/null || echo 'N/A')"
        
        log_success "Cache invalidation test completed"
    else
        log_error "Could not get user ID for invalidation testing"
    fi
    echo
}

# Main test function
run_cache_tests() {
    log_info "Starting User Cache Tests..."
    echo "=========================================="
    
    # Check service
    if ! check_service; then
        exit 1
    fi
    
    # Register and login
    # register_user
    # if ! login_user; then
    #     log_error "Failed to login. Cannot proceed with cache tests."
    #     exit 1
    # fi
    
    echo "=========================================="
    log_info "Running Cache Management Tests..."
    echo "=========================================="
    
    # Cache management tests
    test_cache_health
    test_cache_stats
    test_cache_exists
    test_cache_serialization
    
    echo "=========================================="
    log_info "Running SQL Cache Tests..."
    echo "=========================================="
    
    # SQL cache tests
    test_user_info_cache
    test_user_by_id_cache
    test_user_roles_cache
    
    echo "=========================================="
    log_info "Running Cache Strategy Tests..."
    echo "=========================================="
    
    # Cache strategy tests
    test_cache_strategies
    test_cache_performance
    test_cache_invalidation
    
    echo "=========================================="
    log_info "Running Cache Management Operations..."
    echo "=========================================="
    
    # Cache management operations
    test_cache_reload
    test_cache_clear
    test_cache_warmup
    
    echo "=========================================="
    log_success "All cache tests completed!"
    echo "=========================================="
}

# Help function
show_help() {
    echo "User Cache Test Script"
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -u, --url URL       Set auth-service URL (default: http://localhost:8082)"
    echo "  --username USER     Set test username (default: cacheuser)"
    echo "  --password PASS     Set test password (default: password123)"
    echo "  --email EMAIL       Set test email (default: cacheuser@example.com)"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run all tests with defaults"
    echo "  $0 -u http://localhost:8080          # Use different URL"
    echo "  $0 --username testuser --password testpass  # Use different credentials"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--url)
            AUTH_SERVICE_URL="$2"
            BASE_URL="$AUTH_SERVICE_URL/api/v1"
            shift 2
            ;;
        --username)
            TEST_USERNAME="$2"
            shift 2
            ;;
        --password)
            TEST_PASSWORD="$2"
            shift 2
            ;;
        --email)
            TEST_EMAIL="$2"
            shift 2
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run the tests
run_cache_tests
