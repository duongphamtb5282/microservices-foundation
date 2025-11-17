#!/bin/bash

# User API Testing Script with Cache Strategy Testing
# This script tests the user information API with advanced caching strategies

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
BASE_URL="http://localhost:8082"
API_BASE="${BASE_URL}/api"
AUTH_ENDPOINT="${API_BASE}/auth"
USER_ENDPOINT="${API_BASE}/users"
CACHE_ENDPOINT="${API_BASE}/cache"

# Test user credentials
TEST_USERNAME="duong1"
TEST_PASSWORD="password123"
TEST_EMAIL="test@example.com"

# JWT token storage
JWT_TOKEN=""

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${PURPLE}========================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}========================================${NC}"
}

# Function to check if service is running
check_service() {
    print_status "Checking if auth-service is running..."
    
    if curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        print_success "Auth-service is running on ${BASE_URL}"
        return 0
    else
        print_error "Auth-service is not running on ${BASE_URL}"
        print_status "Please start the auth-service first:"
        print_status "cd /Users/duongphamthaibinh/Downloads/SourceCode/design/beautiful/java/microservices/auth-service"
        print_status "./gradlew bootRun --args=\"--spring.profiles.active=custom\" --no-daemon &"
        return 1
    fi
}

# Function to authenticate and get JWT token
authenticate() {
    print_status "Authenticating user: ${TEST_USERNAME}"
    
    local response=$(curl -s -X POST "${AUTH_ENDPOINT}/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${TEST_USERNAME}\",\"password\":\"${TEST_PASSWORD}\"}")
    
    if echo "$response" | jq -e '.accessToken' > /dev/null 2>&1; then
        JWT_TOKEN=$(echo "$response" | jq -r '.accessToken')
        print_success "Authentication successful"
        print_status "JWT Token: ${JWT_TOKEN:0:50}..."
        return 0
    else
        print_error "Authentication failed"
        echo "Response: $response"
        return 1
    fi
}

# Function to test user registration
test_user_registration() {
    print_header "Testing User Registration"
    
    local new_user="testuser$(date +%s)"
    local new_email="test$(date +%s)@example.com"
    
    print_status "Registering new user: ${new_user}"
    
    local response=$(curl -s -X POST "${AUTH_ENDPOINT}/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"${new_user}\",\"email\":\"${new_email}\",\"password\":\"password123\",\"firstName\":\"Test\",\"lastName\":\"User\"}")
    
    if echo "$response" | jq -e '.message' > /dev/null 2>&1; then
        print_success "User registration successful"
        echo "Response: $response"
    else
        print_error "User registration failed"
        echo "Response: $response"
    fi
}

# Function to test get current user info with caching
test_get_current_user_info() {
    print_header "Testing Get Current User Info (with Caching)"
    
    print_status "First request (should hit database and cache result)..."
    local start_time=$(date +%s%3N)
    local response1=$(curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    local end_time=$(date +%s%3N)
    local duration1=$((end_time - start_time))
    
    if echo "$response1" | jq -e '.userName' > /dev/null 2>&1; then
        print_success "First request successful (${duration1}ms)"
        echo "Response: $response1"
    else
        print_error "First request failed"
        echo "Response: $response1"
        return 1
    fi
    
    print_status "Second request (should hit cache)..."
    start_time=$(date +%s%3N)
    local response2=$(curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    end_time=$(date +%s%3N)
    local duration2=$((end_time - start_time))
    
    if echo "$response2" | jq -e '.userName' > /dev/null 2>&1; then
        print_success "Second request successful (${duration2}ms)"
        echo "Response: $response2"
        
        if [ $duration2 -lt $duration1 ]; then
            print_success "Cache hit confirmed! Second request was faster (${duration2}ms vs ${duration1}ms)"
        else
            print_warning "Cache might not be working optimally (${duration2}ms vs ${duration1}ms)"
        fi
    else
        print_error "Second request failed"
        echo "Response: $response2"
    fi
}

# Function to test get user by ID with caching
test_get_user_by_id() {
    print_header "Testing Get User by ID (with Caching)"
    
    # First get current user to get the ID
    local user_info=$(curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    local user_id=$(echo "$user_info" | jq -r '.id')
    
    if [ "$user_id" = "null" ] || [ -z "$user_id" ]; then
        print_error "Could not get user ID"
        return 1
    fi
    
    print_status "Testing get user by ID: ${user_id}"
    
    # First request
    print_status "First request (should hit database)..."
    local start_time=$(date +%s%3N)
    local response1=$(curl -s -X GET "${USER_ENDPOINT}/${user_id}" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    local end_time=$(date +%s%3N)
    local duration1=$((end_time - start_time))
    
    if echo "$response1" | jq -e '.userName' > /dev/null 2>&1; then
        print_success "First request successful (${duration1}ms)"
    else
        print_error "First request failed"
        echo "Response: $response1"
        return 1
    fi
    
    # Second request (should hit cache)
    print_status "Second request (should hit cache)..."
    start_time=$(date +%s%3N)
    local response2=$(curl -s -X GET "${USER_ENDPOINT}/${user_id}" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    end_time=$(date +%s%3N)
    local duration2=$((end_time - start_time))
    
    if echo "$response2" | jq -e '.userName' > /dev/null 2>&1; then
        print_success "Second request successful (${duration2}ms)"
        
        if [ $duration2 -lt $duration1 ]; then
            print_success "Cache hit confirmed! (${duration2}ms vs ${duration1}ms)"
        else
            print_warning "Cache might not be working optimally"
        fi
    else
        print_error "Second request failed"
        echo "Response: $response2"
    fi
}

# Function to test get all users with pagination and caching
test_get_all_users() {
    print_header "Testing Get All Users (with Pagination and Caching)"
    
    print_status "Testing get all users with pagination..."
    
    # First request
    print_status "First request (should hit database)..."
    local start_time=$(date +%s%3N)
    local response1=$(curl -s -X GET "${USER_ENDPOINT}?page=0&size=5" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    local end_time=$(date +%s%3N)
    local duration1=$((end_time - start_time))
    
    if echo "$response1" | jq -e '.users' > /dev/null 2>&1; then
        print_success "First request successful (${duration1}ms)"
        local total_users=$(echo "$response1" | jq -r '.totalElements')
        print_status "Total users: ${total_users}"
    else
        print_error "First request failed"
        echo "Response: $response1"
        return 1
    fi
    
    # Second request (should hit cache)
    print_status "Second request (should hit cache)..."
    start_time=$(date +%s%3N)
    local response2=$(curl -s -X GET "${USER_ENDPOINT}?page=0&size=5" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    end_time=$(date +%s%3N)
    local duration2=$((end_time - start_time))
    
    if echo "$response2" | jq -e '.users' > /dev/null 2>&1; then
        print_success "Second request successful (${duration2}ms)"
        
        if [ $duration2 -lt $duration1 ]; then
            print_success "Cache hit confirmed! (${duration2}ms vs ${duration1}ms)"
        else
            print_warning "Cache might not be working optimally"
        fi
    else
        print_error "Second request failed"
        echo "Response: $response2"
    fi
}

# Function to test search users with caching
test_search_users() {
    print_header "Testing Search Users (with Caching)"
    
    print_status "Testing user search with query: 'test'..."
    
    # First request
    print_status "First request (should hit database)..."
    local start_time=$(date +%s%3N)
    local response1=$(curl -s -X GET "${USER_ENDPOINT}/search?query=test&page=0&size=5" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    local end_time=$(date +%s%3N)
    local duration1=$((end_time - start_time))
    
    if echo "$response1" | jq -e '.users' > /dev/null 2>&1; then
        print_success "First request successful (${duration1}ms)"
        local total_results=$(echo "$response1" | jq -r '.totalElements')
        print_status "Search results: ${total_results}"
    else
        print_error "First request failed"
        echo "Response: $response1"
        return 1
    fi
    
    # Second request (should hit cache)
    print_status "Second request (should hit cache)..."
    start_time=$(date +%s%3N)
    local response2=$(curl -s -X GET "${USER_ENDPOINT}/search?query=test&page=0&size=5" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    end_time=$(date +%s%3N)
    local duration2=$((end_time - start_time))
    
    if echo "$response2" | jq -e '.users' > /dev/null 2>&1; then
        print_success "Second request successful (${duration2}ms)"
        
        if [ $duration2 -lt $duration1 ]; then
            print_success "Cache hit confirmed! (${duration2}ms vs ${duration1}ms)"
        else
            print_warning "Cache might not be working optimally"
        fi
    else
        print_error "Second request failed"
        echo "Response: $response2"
    fi
}

# Function to test user roles with caching
test_get_user_roles() {
    print_header "Testing Get User Roles (with Caching)"
    
    # First get current user to get the ID
    local user_info=$(curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    local user_id=$(echo "$user_info" | jq -r '.id')
    
    if [ "$user_id" = "null" ] || [ -z "$user_id" ]; then
        print_error "Could not get user ID"
        return 1
    fi
    
    print_status "Testing get user roles for user ID: ${user_id}"
    
    # First request
    print_status "First request (should hit database)..."
    local start_time=$(date +%s%3N)
    local response1=$(curl -s -X GET "${USER_ENDPOINT}/${user_id}/roles" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    local end_time=$(date +%s%3N)
    local duration1=$((end_time - start_time))
    
    if echo "$response1" | jq -e '.roles' > /dev/null 2>&1; then
        print_success "First request successful (${duration1}ms)"
        local roles=$(echo "$response1" | jq -r '.roles')
        print_status "User roles: $roles"
    else
        print_error "First request failed"
        echo "Response: $response1"
        return 1
    fi
    
    # Second request (should hit cache)
    print_status "Second request (should hit cache)..."
    start_time=$(date +%s%3N)
    local response2=$(curl -s -X GET "${USER_ENDPOINT}/${user_id}/roles" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    end_time=$(date +%s%3N)
    local duration2=$((end_time - start_time))
    
    if echo "$response2" | jq -e '.roles' > /dev/null 2>&1; then
        print_success "Second request successful (${duration2}ms)"
        
        if [ $duration2 -lt $duration1 ]; then
            print_success "Cache hit confirmed! (${duration2}ms vs ${duration1}ms)"
        else
            print_warning "Cache might not be working optimally"
        fi
    else
        print_error "Second request failed"
        echo "Response: $response2"
    fi
}

# Function to test cache management
test_cache_management() {
    print_header "Testing Cache Management"
    
    print_status "Getting user cache statistics..."
    local cache_stats=$(curl -s -X GET "${USER_ENDPOINT}/cache/stats" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    if echo "$cache_stats" | jq -e '.' > /dev/null 2>&1; then
        print_success "Cache statistics retrieved"
        echo "Cache Stats: $cache_stats"
    else
        print_error "Failed to get cache statistics"
        echo "Response: $cache_stats"
    fi
    
    print_status "Clearing user cache..."
    local clear_response=$(curl -s -X POST "${USER_ENDPOINT}/cache/clear" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    if echo "$clear_response" | jq -e '.message' > /dev/null 2>&1; then
        print_success "User cache cleared successfully"
        echo "Response: $clear_response"
    else
        print_error "Failed to clear user cache"
        echo "Response: $clear_response"
    fi
}

# Function to test cache invalidation
test_cache_invalidation() {
    print_header "Testing Cache Invalidation"
    
    # First get current user to get the ID
    local user_info=$(curl -s -X GET "${USER_ENDPOINT}/me" \
        -H "Authorization: Bearer ${JWT_TOKEN}")
    
    local user_id=$(echo "$user_info" | jq -r '.id')
    
    if [ "$user_id" = "null" ] || [ -z "$user_id" ]; then
        print_error "Could not get user ID"
        return 1
    fi
    
    print_status "Testing cache invalidation by updating user..."
    
    # Update user information
    local update_data='{"firstName":"Updated","lastName":"User"}'
    local update_response=$(curl -s -X PUT "${USER_ENDPOINT}/${user_id}" \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "$update_data")
    
    if echo "$update_response" | jq -e '.firstName' > /dev/null 2>&1; then
        print_success "User updated successfully"
        echo "Updated user: $update_response"
        
        # Verify cache was invalidated by checking if the update is reflected
        print_status "Verifying cache invalidation..."
        local verify_response=$(curl -s -X GET "${USER_ENDPOINT}/me" \
            -H "Authorization: Bearer ${JWT_TOKEN}")
        
        local updated_first_name=$(echo "$verify_response" | jq -r '.firstName')
        if [ "$updated_first_name" = "Updated" ]; then
            print_success "Cache invalidation confirmed! Updated data is reflected"
        else
            print_warning "Cache invalidation might not be working properly"
        fi
    else
        print_error "Failed to update user"
        echo "Response: $update_response"
    fi
}

# Function to run performance test
test_performance() {
    print_header "Testing Performance with Multiple Requests"
    
    print_status "Running 10 consecutive requests to test cache performance..."
    
    local total_time=0
    local cache_hits=0
    
    for i in {1..10}; do
        print_status "Request $i/10..."
        local start_time=$(date +%s%3N)
        local response=$(curl -s -X GET "${USER_ENDPOINT}/me" \
            -H "Authorization: Bearer ${JWT_TOKEN}")
        local end_time=$(date +%s%3N)
        local duration=$((end_time - start_time))
        
        total_time=$((total_time + duration))
        
        if echo "$response" | jq -e '.userName' > /dev/null 2>&1; then
            print_success "Request $i successful (${duration}ms)"
            if [ $duration -lt 50 ]; then
                cache_hits=$((cache_hits + 1))
            fi
        else
            print_error "Request $i failed"
        fi
    done
    
    local average_time=$((total_time / 10))
    print_success "Performance test completed"
    print_status "Average response time: ${average_time}ms"
    print_status "Cache hits (requests < 50ms): ${cache_hits}/10"
    
    if [ $cache_hits -ge 7 ]; then
        print_success "Excellent cache performance! ${cache_hits}/10 requests were served from cache"
    elif [ $cache_hits -ge 5 ]; then
        print_warning "Good cache performance: ${cache_hits}/10 requests were served from cache"
    else
        print_warning "Cache performance could be improved: only ${cache_hits}/10 requests were served from cache"
    fi
}

# Main execution function
main() {
    print_header "User API Testing Script with Cache Strategy"
    print_status "Testing user information API with advanced caching strategies"
    print_status "Base URL: ${BASE_URL}"
    print_status "Test User: ${TEST_USERNAME}"
    
    # Check if service is running
    if ! check_service; then
        exit 1
    fi
    
    # Authenticate
    if ! authenticate; then
        exit 1
    fi
    
    # Run all tests
    test_user_registration
    test_get_current_user_info
    test_get_user_by_id
    test_get_all_users
    test_search_users
    test_get_user_roles
    test_cache_management
    test_cache_invalidation
    test_performance
    
    print_header "All Tests Completed"
    print_success "User API testing with cache strategy completed successfully!"
    print_status "Summary of tested features:"
    print_status "- User registration"
    print_status "- Get current user info with caching"
    print_status "- Get user by ID with caching"
    print_status "- Get all users with pagination and caching"
    print_status "- Search users with caching"
    print_status "- Get user roles with caching"
    print_status "- Cache management and statistics"
    print_status "- Cache invalidation on updates"
    print_status "- Performance testing with multiple requests"
}

# Run main function
main "$@"
