#!/bin/bash

# Test /users/me endpoint with Keycloak authentication
# This script tests both /api/auth/me and /api/users/me endpoints

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Testing /users/me with Keycloak${NC}"
echo -e "${BLUE}========================================${NC}\n"


# Configuration
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
# REALM="${KEYCLOAK_REALM:-auth-service}"
REALM="auth-service"
CLIENT_ID="${KEYCLOAK_CLIENT_ID:-auth-service-client}"
CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET:-your-client-secret-here}"
AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8082}"

# Test user credentials
USERNAME="${TEST_USERNAME:-testuser}"
PASSWORD="${TEST_PASSWORD:-password123}"

echo -e "${YELLOW}Configuration:${NC}"
echo "Keycloak URL: $KEYCLOAK_URL"
echo "Realm: $REALM"
echo "Client ID: $CLIENT_ID"
echo "Auth Service URL: $AUTH_SERVICE_URL"
echo "Test User: $USERNAME"
echo ""

# Function to check if service is running
check_service() {
    local url=$1
    local name=$2
    
    echo -e "${BLUE}Checking if $name is running...${NC}"
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ $name is running${NC}\n"
        return 0
    else
        echo -e "${RED}❌ $name is not running at $url${NC}"
        echo -e "${YELLOW}Please start $name first${NC}\n"
        return 1
    fi
}

# Check if Keycloak is running
if ! check_service "$KEYCLOAK_URL" "Keycloak"; then
    echo -e "${YELLOW}To start Keycloak, run:${NC}"
    echo "cd auth-service && docker-compose up -d keycloak"
    exit 1
fi

# Check if Auth Service is running
if ! check_service "$AUTH_SERVICE_URL/actuator/health" "Auth Service"; then
    echo -e "${YELLOW}To start Auth Service, run:${NC}"
    echo "cd auth-service && ./gradlew bootRun --args='--spring.profiles.active=dev'"
    exit 1
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 1: Get Keycloak Token${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Get token from Keycloak
echo -e "${YELLOW}Requesting token for user: $USERNAME${NC}"

TOKEN_RESPONSE=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password")

# Check if token request was successful
if echo "$TOKEN_RESPONSE" | grep -q "error"; then
    echo -e "${RED}❌ Failed to get token from Keycloak${NC}"
    echo -e "${RED}Error response:${NC}"
    echo "$TOKEN_RESPONSE" | jq '.'
    echo ""
    echo -e "${YELLOW}Possible issues:${NC}"
    echo "1. User '$USERNAME' doesn't exist in Keycloak"
    echo "2. Wrong password"
    echo "3. Client credentials are incorrect"
    echo "4. Realm name is incorrect"
    echo ""
    echo -e "${YELLOW}To create test user in Keycloak:${NC}"
    echo "1. Go to $KEYCLOAK_URL/admin"
    echo "2. Login with admin credentials (admin/admin)"
    echo "3. Select realm '$REALM'"
    echo "4. Go to Users > Add User"
    echo "5. Set username to '$USERNAME'"
    echo "6. Save and set password in Credentials tab"
    exit 1
fi

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.refresh_token')
EXPIRES_IN=$(echo "$TOKEN_RESPONSE" | jq -r '.expires_in')

if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}❌ Failed to extract access token${NC}"
    echo "Response:"
    echo "$TOKEN_RESPONSE" | jq '.'
    exit 1
fi

echo -e "${GREEN}✅ Token obtained successfully${NC}"
echo "Token expires in: ${EXPIRES_IN}s"
echo ""
echo -e "${YELLOW}Access Token (first 50 chars):${NC}"
echo "${ACCESS_TOKEN}"
echo ""

# Decode token to show claims
# echo -e "${YELLOW}Token Claims:${NC}"
# echo "$ACCESS_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.' || echo "Could not decode token"
# echo ""

echo -e "${YELLOW}Token Claims:${NC}"
echo "$ACCESS_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.' || \
(echo "$ACCESS_TOKEN" | cut -d'.' -f2 | tr '_-' '/+' | base64 -d -i 2>/dev/null | jq '.' || \
echo "Could not decode token")

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 2: Test /api/auth/me${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${YELLOW}Testing endpoint: GET /api/auth/me${NC}"
echo "This endpoint extracts user info directly from JWT token"
echo ""

AUTH_ME_RESPONSE=$(curl -s -X GET \
  "$AUTH_SERVICE_URL/api/auth/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json")

echo -e "${GREEN}Response:${NC}"
echo "$AUTH_ME_RESPONSE" | jq '.'
echo ""

# Check if response is successful
if echo "$AUTH_ME_RESPONSE" | jq -e '.username' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ /api/auth/me endpoint works!${NC}"
    USERNAME_FROM_TOKEN=$(echo "$AUTH_ME_RESPONSE" | jq -r '.username')
    echo "Username from token: $USERNAME_FROM_TOKEN"
else
    echo -e "${RED}❌ /api/auth/me endpoint failed${NC}"
    echo "Response: $AUTH_ME_RESPONSE"
fi
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 3: Test /api/users/me${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${YELLOW}Testing endpoint: GET /api/users/me${NC}"
echo "This endpoint fetches user info from the database"
echo ""

USERS_ME_RESPONSE=$(curl -s -X GET \
  "$AUTH_SERVICE_URL/api/users/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -w "\nHTTP_STATUS:%{http_code}")

HTTP_STATUS=$(echo "$USERS_ME_RESPONSE" | grep "HTTP_STATUS" | cut -d':' -f2)
RESPONSE_BODY=$(echo "$USERS_ME_RESPONSE" | sed '/HTTP_STATUS/d')

echo -e "${GREEN}Response (Status: $HTTP_STATUS):${NC}"
if [ "$HTTP_STATUS" == "200" ]; then
    echo "$RESPONSE_BODY" | jq '.'
    echo ""
    echo -e "${GREEN}✅ /api/users/me endpoint works!${NC}"
elif [ "$HTTP_STATUS" == "404" ]; then
    echo "$RESPONSE_BODY"
    echo ""
    echo -e "${YELLOW}⚠️  User not found in database${NC}"
    echo "This is expected if the user only exists in Keycloak but not in the local database."
    echo ""
    echo -e "${YELLOW}To sync the user to local database:${NC}"
    echo "1. Register the user via /api/auth/register endpoint, OR"
    echo "2. Implement user synchronization from Keycloak to local DB"
elif [ "$HTTP_STATUS" == "401" ]; then
    echo "$RESPONSE_BODY"
    echo ""
    echo -e "${RED}❌ Unauthorized - Token not accepted${NC}"
else
    echo "$RESPONSE_BODY"
    echo ""
    echo -e "${RED}❌ /api/users/me endpoint failed with status $HTTP_STATUS${NC}"
fi
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 4: Token Introspection (Optional)${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${YELLOW}Introspecting token with Keycloak...${NC}"

INTROSPECT_RESPONSE=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token/introspect" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "token=$ACCESS_TOKEN")

echo -e "${GREEN}Introspection Response:${NC}"
echo "$INTROSPECT_RESPONSE" | jq '.'
echo ""

IS_ACTIVE=$(echo "$INTROSPECT_RESPONSE" | jq -r '.active')
if [ "$IS_ACTIVE" == "true" ]; then
    echo -e "${GREEN}✅ Token is active and valid${NC}"
else
    echo -e "${RED}❌ Token is not active${NC}"
fi
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 5: Summary${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${YELLOW}Test Results:${NC}"
echo "1. Keycloak Token: ✅ Obtained"
echo "2. /api/auth/me: $([ $(echo "$AUTH_ME_RESPONSE" | jq -e '.username' > /dev/null 2>&1; echo $?) -eq 0 ] && echo '✅ Success' || echo '❌ Failed')"
echo "3. /api/users/me: $([ "$HTTP_STATUS" == "200" ] && echo '✅ Success' || echo "⚠️  Status: $HTTP_STATUS")"
echo "4. Token Introspection: $([ "$IS_ACTIVE" == "true" ] && echo '✅ Active' || echo '❌ Inactive')"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Test completed!${NC}"
echo -e "${GREEN}========================================${NC}"

# Save token to file for manual testing
echo "$ACCESS_TOKEN" > /tmp/keycloak-token.txt
echo ""
echo -e "${YELLOW}Access token saved to: /tmp/keycloak-token.txt${NC}"
echo -e "${YELLOW}You can use it for manual testing:${NC}"
echo "export TOKEN=\$(cat /tmp/keycloak-token.txt)"
echo "curl -H \"Authorization: Bearer \$TOKEN\" $AUTH_SERVICE_URL/api/auth/me"

