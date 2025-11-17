#!/bin/bash

# Debug JWT Authentication Script
# This script helps debug JWT authentication issues

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AUTH_SERVICE_URL="http://localhost:8082"
BASE_URL="$AUTH_SERVICE_URL/api"

# Logging functions
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

# Test application health
test_health() {
    log_info "Testing application health..."
    local response=$(curl -s -X GET "$AUTH_SERVICE_URL/actuator/health")
    echo "Health Response: $response"
    echo
}

# Test login
test_login() {
    log_info "Testing login..."
    local response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "testuser2",
            "password": "password123"
        }')
    
    echo "Login Response: $response"
    
    # Extract JWT token
    JWT_TOKEN=$(echo "$response" | jq -r '.accessToken' 2>/dev/null)
    
    if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
        log_success "JWT token obtained: ${JWT_TOKEN:0:50}..."
        return 0
    else
        log_error "Failed to get JWT token"
        return 1
    fi
}

# Test JWT token validation
test_jwt_validation() {
    if [ -z "$JWT_TOKEN" ]; then
        log_error "No JWT token available"
        return 1
    fi
    
    log_info "Testing JWT token validation..."
    
    # Decode JWT token (header and payload)
    local header=$(echo "$JWT_TOKEN" | cut -d'.' -f1 | base64 -d 2>/dev/null)
    local payload=$(echo "$JWT_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null)
    
    echo "JWT Header: $header"
    echo "JWT Payload: $payload"
    echo
}

# Test protected endpoint
test_protected_endpoint() {
    if [ -z "$JWT_TOKEN" ]; then
        log_error "No JWT token available"
        return 1
    fi
    
    log_info "Testing protected endpoint /api/users/me..."
    
    local response=$(curl -s -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN")
    
    echo "Protected endpoint response: $response"
    echo
    
    # Check if we got user info
    local user_id=$(echo "$response" | jq -r '.id' 2>/dev/null)
    if [ -n "$user_id" ] && [ "$user_id" != "null" ]; then
        log_success "User ID extracted: $user_id"
        return 0
    else
        log_error "Could not extract user ID from response"
        return 1
    fi
}

# Test with verbose curl
test_verbose() {
    if [ -z "$JWT_TOKEN" ]; then
        log_error "No JWT token available"
        return 1
    fi
    
    log_info "Testing with verbose curl..."
    
    curl -v -X GET "$BASE_URL/users/me" \
        -H "Authorization: Bearer $JWT_TOKEN"
}

# Main execution
main() {
    echo "=== JWT Authentication Debug Script ==="
    echo
    
    # Test 1: Health check
    test_health
    
    # Test 2: Login
    if test_login; then
        # Test 3: JWT validation
        test_jwt_validation
        
        # Test 4: Protected endpoint
        if test_protected_endpoint; then
            log_success "JWT authentication is working!"
        else
            log_error "JWT authentication failed"
            # Test 5: Verbose debugging
            test_verbose
        fi
    else
        log_error "Login failed, cannot test JWT authentication"
    fi
}

# Run main function
main "$@"

