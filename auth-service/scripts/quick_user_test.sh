#!/bin/bash

# Quick User API Test Script
# Simple script to test user API endpoints with caching

set -e

# Configuration
BASE_URL="http://localhost:8082"
API_BASE="${BASE_URL}/api"
AUTH_ENDPOINT="${API_BASE}/auth"
USER_ENDPOINT="${API_BASE}/users"

# Test user credentials
TEST_USERNAME="duong"
TEST_PASSWORD="password123"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if service is running
check_service() {
    print_status "Checking if auth-service is running..."
    if curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        print_success "Auth-service is running"
        return 0
    else
        print_error "Auth-service is not running. Please start it first."
        return 1
    fi
}

# Authenticate and get token
authenticate() {
    print_status "Authenticating user: ${TEST_USERNAME}"
    
    local response=$(curl -s -X POST "${AUTH_ENDPOINT}/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${TEST_USERNAME}\",\"password\":\"${TEST_PASSWORD}\"}")
    
    if echo "$response" | jq -e '.accessToken' > /dev/null 2>&1; then
        JWT_TOKEN=$(echo "$response" | jq -r '.accessToken')
        print_success "Authentication successful"
        return 0
    else
        print_error "Authentication failed"
        echo "Response: $response"
        return 1
    fi
}

# Test get current user info
test_get_current_user() {
    print_status "Testing get current user info..."
    
    local response=$(curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    if echo "$response" | jq -e '.userName' > /dev/null 2>&1; then
        print_success "Get current user successful"
        echo "User: $(echo "$response" | jq -r '.userName')"
        echo "Email: $(echo "$response" | jq -r '.email')"
        echo "Roles: $(echo "$response" | jq -r '.roles')"
    else
        print_error "Get current user failed"
        echo "Response: $response"
    fi
}

# Test get all users
test_get_all_users() {
    print_status "Testing get all users..."
    
    local response=$(curl -s -X GET "${USER_ENDPOINT}?page=0&size=5" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    if echo "$response" | jq -e '.users' > /dev/null 2>&1; then
        print_success "Get all users successful"
        local total=$(echo "$response" | jq -r '.totalElements')
        print_status "Total users: $total"
    else
        print_error "Get all users failed"
        echo "Response: $response"
    fi
}

# Test search users
test_search_users() {
    print_status "Testing search users..."
    
    local response=$(curl -s -X GET "${USER_ENDPOINT}/search?query=test&page=0&size=5" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    if echo "$response" | jq -e '.users' > /dev/null 2>&1; then
        print_success "Search users successful"
        local total=$(echo "$response" | jq -r '.totalElements')
        print_status "Search results: $total"
    else
        print_error "Search users failed"
        echo "Response: $response"
    fi
}

# Test cache performance
test_cache_performance() {
    print_status "Testing cache performance..."
    
    print_status "First request (should hit database)..."
    local start_time=$(date +%s%3N)
    curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}" > /dev/null
    local end_time=$(date +%s%3N)
    local duration1=$((end_time - start_time))
    print_status "First request: ${duration1}ms"
    
    print_status "Second request (should hit cache)..."
    start_time=$(date +%s%3N)
    curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}" > /dev/null
    end_time=$(date +%s%3N)
    local duration2=$((end_time - start_time))
    print_status "Second request: ${duration2}ms"
    
    if [ $duration2 -lt $duration1 ]; then
        print_success "Cache hit confirmed! Second request was faster (${duration2}ms vs ${duration1}ms)"
    else
        print_status "Cache performance: ${duration2}ms vs ${duration1}ms"
    fi
}

# Main execution
main() {
    echo "=========================================="
    echo "Quick User API Test Script"
    echo "=========================================="
    
    if ! check_service; then
        exit 1
    fi
    
    if ! authenticate; then
        exit 1
    fi
    
    test_get_current_user
    test_get_all_users
    test_search_users
    test_cache_performance
    
    print_success "Quick test completed!"
}

# Run main function
main "$@"
