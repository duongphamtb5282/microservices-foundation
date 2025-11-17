#!/bin/bash

# User API Curl Commands Reference
# This script contains all the curl commands for testing the user API

# Configuration
BASE_URL="http://localhost:8082"
API_BASE="${BASE_URL}/api"
AUTH_ENDPOINT="${API_BASE}/auth"
USER_ENDPOINT="${API_BASE}/users"

# Test credentials
TEST_USERNAME="testuser"
TEST_PASSWORD="password123"

echo "=========================================="
echo "User API Curl Commands Reference"
echo "=========================================="
echo ""

echo "1. AUTHENTICATION"
echo "=================="
echo "Login to get JWT token:"
echo "curl -X POST ${AUTH_ENDPOINT}/login \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"username\":\"${TEST_USERNAME}\",\"password\":\"${TEST_PASSWORD}\"}'"
echo ""

echo "2. USER INFORMATION ENDPOINTS"
echo "============================="
echo ""

echo "Get current user info (with caching):"
echo "curl -X GET ${USER_ENDPOINT}/me \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "Get user by ID (with caching):"
echo "curl -X GET ${USER_ENDPOINT}/{userId} \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "Get user by username (with caching):"
echo "curl -X GET ${USER_ENDPOINT}/username/{username} \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "Get all users with pagination (with caching):"
echo "curl -X GET \"${USER_ENDPOINT}?page=0&size=10\" \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "Search users (with caching):"
echo "curl -X GET \"${USER_ENDPOINT}/search?query=test&page=0&size=10\" \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "Get user roles (with caching):"
echo "curl -X GET ${USER_ENDPOINT}/{userId}/roles \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "3. USER MANAGEMENT ENDPOINTS"
echo "============================="
echo ""

echo "Update user information (invalidates cache):"
echo "curl -X PUT ${USER_ENDPOINT}/{userId} \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"firstName\":\"Updated\",\"lastName\":\"Name\"}'"
echo ""

echo "Delete user (invalidates cache):"
echo "curl -X DELETE ${USER_ENDPOINT}/{userId} \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "4. CACHE MANAGEMENT ENDPOINTS"
echo "=============================="
echo ""

echo "Get user cache statistics:"
echo "curl -X GET ${USER_ENDPOINT}/cache/stats \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "Clear user cache:"
echo "curl -X POST ${USER_ENDPOINT}/cache/clear \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "5. CACHE STRATEGY TESTING"
echo "========================="
echo ""

echo "Test cache performance (run multiple times):"
echo "time curl -X GET ${USER_ENDPOINT}/me \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "Test cache invalidation:"
echo "# First, get user info"
echo "curl -X GET ${USER_ENDPOINT}/me \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""
echo "# Update user (this should invalidate cache)"
echo "curl -X PUT ${USER_ENDPOINT}/{userId} \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"firstName\":\"NewName\"}'"
echo ""
echo "# Get user info again (should reflect changes)"
echo "curl -X GET ${USER_ENDPOINT}/me \\"
echo "  -H \"Authorization: Bearer YOUR_JWT_TOKEN\""
echo ""

echo "6. COMPLETE TESTING WORKFLOW"
echo "============================="
echo ""

echo "# Step 1: Authenticate"
echo "TOKEN=\$(curl -s -X POST ${AUTH_ENDPOINT}/login \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -d '{\"username\":\"${TEST_USERNAME}\",\"password\":\"${TEST_PASSWORD}\"}' | \\"
echo "  jq -r '.accessToken')"
echo ""

echo "# Step 2: Test user info with caching"
echo "echo \"First request (should hit database):\""
echo "time curl -X GET ${USER_ENDPOINT}/me \\"
echo "  -H \"Authorization: Bearer \$TOKEN\""
echo ""
echo "echo \"Second request (should hit cache):\""
echo "time curl -X GET ${USER_ENDPOINT}/me \\"
echo "  -H \"Authorization: Bearer \$TOKEN\""
echo ""

echo "# Step 3: Test search with caching"
echo "echo \"Search first request (should hit database):\""
echo "time curl -X GET \"${USER_ENDPOINT}/search?query=test&page=0&size=5\" \\"
echo "  -H \"Authorization: Bearer \$TOKEN\""
echo ""
echo "echo \"Search second request (should hit cache):\""
echo "time curl -X GET \"${USER_ENDPOINT}/search?query=test&page=0&size=5\" \\"
echo "  -H \"Authorization: Bearer \$TOKEN\""
echo ""

echo "# Step 4: Test cache management"
echo "echo \"Cache statistics:\""
echo "curl -X GET ${USER_ENDPOINT}/cache/stats \\"
echo "  -H \"Authorization: Bearer \$TOKEN\""
echo ""

echo "7. CACHE CONFIGURATION DETAILS"
echo "==============================="
echo ""

echo "Cache TTL Settings:"
echo "- user-info: 5 minutes (L1), 10 minutes (L2)"
echo "- user-by-id: 10 minutes (L1), 15 minutes (L2)"
echo "- user-by-username: 10 minutes (L1), 15 minutes (L2)"
echo "- all-users: 2 minutes (L1), 5 minutes (L2)"
echo "- user-search: 3 minutes (L1), 5 minutes (L2)"
echo "- user-roles: 15 minutes (L1), 30 minutes (L2)"
echo "- user-stats: 30 minutes (L1), 1 hour (L2)"
echo ""

echo "Cache Invalidation:"
echo "- Update user: Invalidates user-info, user-by-id, user-by-username, all-users, user-search"
echo "- Delete user: Invalidates all user-related caches"
echo "- Clear cache: Manually clears all user caches"
echo ""

echo "8. PERFORMANCE TESTING"
echo "==================="
echo ""

echo "Run multiple requests to test cache performance:"
echo "for i in {1..10}; do"
echo "  echo \"Request \$i:\""
echo "  time curl -s -X GET ${USER_ENDPOINT}/me \\"
echo "    -H \"Authorization: Bearer \$TOKEN\" > /dev/null"
echo "done"
echo ""

echo "=========================================="
echo "End of User API Curl Commands Reference"
echo "=========================================="
