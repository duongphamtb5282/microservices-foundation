#!/bin/bash

# Keycloak Integration Test Script
# Tests Keycloak authentication with auth-service

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
KEYCLOAK_URL="http://localhost:8080"
AUTH_SERVICE_URL="http://localhost:8082"
KEYCLOAK_REALM="master"
CUSTOM_REALM="auth-service"
CLIENT_ID="auth-service-client"
CLIENT_SECRET="your-client-secret-here"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin123"
TEST_USERNAME="testuser"
TEST_PASSWORD="password123"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🔐 Keycloak Integration Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

# Function to check if service is running
check_service() {
    local url=$1
    local name=$2
    local max_attempts=30
    local attempt=0

    print_status "Checking if $name is running..."
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            print_success "$name is running"
            return 0
        fi
        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done
    
    echo ""
    print_error "$name is not running after $max_attempts attempts"
    return 1
}

# Step 1: Start Docker Services
echo -e "\n${BLUE}Step 1: Starting Docker Services${NC}"
echo "================================================"

print_status "Starting PostgreSQL, Redis, Kafka, and Keycloak..."
cd /Users/duongphamthaibinh/Downloads/SourceCode/design/beautiful/java/microservices/auth-service

# Start services
docker-compose up -d postgres redis zookeeper kafka keycloak

print_success "Docker services started"
echo ""

# Step 2: Wait for Services
echo -e "\n${BLUE}Step 2: Waiting for Services to be Ready${NC}"
echo "================================================"

# Check PostgreSQL
check_service "http://localhost:5432" "PostgreSQL" || true

# Check Redis
print_status "Checking Redis..."
if docker exec auth-service-redis redis-cli ping > /dev/null 2>&1; then
    print_success "Redis is running"
else
    print_warning "Redis may not be ready yet, continuing..."
fi

# Check Keycloak
check_service "${KEYCLOAK_URL}/health/ready" "Keycloak"

echo ""
print_success "All services are ready!"
sleep 5

# Step 3: Get Keycloak Admin Token
echo -e "\n${BLUE}Step 3: Getting Keycloak Admin Token${NC}"
echo "================================================"

print_status "Requesting admin token from Keycloak..."

ADMIN_TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USERNAME}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

if [ -z "$ADMIN_TOKEN_RESPONSE" ]; then
    print_error "Failed to get admin token"
    exit 1
fi

ADMIN_TOKEN=$(echo $ADMIN_TOKEN_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ADMIN_TOKEN" ]; then
    print_error "Failed to extract admin access token"
    echo "Response: $ADMIN_TOKEN_RESPONSE"
    exit 1
fi

print_success "Admin token obtained successfully"
echo "Token (first 50 chars): ${ADMIN_TOKEN:0:50}..."
echo ""

# Step 4: Check Keycloak Realms
echo -e "\n${BLUE}Step 4: Checking Keycloak Realms${NC}"
echo "================================================"

print_status "Fetching available realms..."

REALMS=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json")

echo "Available Realms:"
echo "$REALMS" | grep -o '"realm":"[^"]*"' | cut -d'"' -f4 | while read realm; do
    echo "  - $realm"
done
echo ""

# Step 5: Get User Token (Direct Grant)
echo -e "\n${BLUE}Step 5: Testing Direct Grant Authentication${NC}"
echo "================================================"

print_status "Getting user token via password grant..."

# Try master realm first
print_status "Attempting authentication with master realm..."
USER_TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" \
  -d "username=${TEST_USERNAME}" \
  -d "password=${TEST_PASSWORD}" \
  -d "grant_type=password" 2>&1)

if echo "$USER_TOKEN_RESPONSE" | grep -q "access_token"; then
    USER_ACCESS_TOKEN=$(echo $USER_TOKEN_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    print_success "User token obtained from master realm"
else
    print_warning "Failed to get token from master realm, trying custom realm..."
    
    # Try custom realm
    USER_TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${CUSTOM_REALM}/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=${CLIENT_ID}" \
      -d "client_secret=${CLIENT_SECRET}" \
      -d "username=${TEST_USERNAME}" \
      -d "password=${TEST_PASSWORD}" \
      -d "grant_type=password" 2>&1)
    
    if echo "$USER_TOKEN_RESPONSE" | grep -q "access_token"; then
        USER_ACCESS_TOKEN=$(echo $USER_TOKEN_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
        print_success "User token obtained from ${CUSTOM_REALM} realm"
    else
        print_error "Failed to get user token from both realms"
        echo "Response: $USER_TOKEN_RESPONSE"
        
        # Create test user in master realm
        print_status "Creating test user in master realm..."
        CREATE_USER_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}/users" \
          -H "Authorization: Bearer ${ADMIN_TOKEN}" \
          -H "Content-Type: application/json" \
          -d '{
            "username": "'${TEST_USERNAME}'",
            "email": "test@auth-service.com",
            "firstName": "Test",
            "lastName": "User",
            "enabled": true,
            "emailVerified": true,
            "credentials": [{
              "type": "password",
              "value": "'${TEST_PASSWORD}'",
              "temporary": false
            }]
          }')
        
        print_success "Test user created, retrying authentication..."
        sleep 2
        
        USER_TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
          -H "Content-Type: application/x-www-form-urlencoded" \
          -d "client_id=${CLIENT_ID}" \
          -d "client_secret=${CLIENT_SECRET}" \
          -d "username=${TEST_USERNAME}" \
          -d "password=${TEST_PASSWORD}" \
          -d "grant_type=password")
        
        USER_ACCESS_TOKEN=$(echo $USER_TOKEN_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    fi
fi

if [ -z "$USER_ACCESS_TOKEN" ]; then
    print_error "Failed to obtain user access token"
    echo "Please check Keycloak configuration"
    echo ""
    echo "Keycloak Admin Console: ${KEYCLOAK_URL}"
    echo "Username: ${ADMIN_USERNAME}"
    echo "Password: ${ADMIN_PASSWORD}"
    exit 1
fi

print_success "User access token obtained"
echo "Token (first 50 chars): ${USER_ACCESS_TOKEN:0:50}..."
echo ""

# Display token details
print_status "Token Details:"
REFRESH_TOKEN=$(echo $USER_TOKEN_RESPONSE | grep -o '"refresh_token":"[^"]*"' | cut -d'"' -f4)
TOKEN_TYPE=$(echo $USER_TOKEN_RESPONSE | grep -o '"token_type":"[^"]*"' | cut -d'"' -f4)
EXPIRES_IN=$(echo $USER_TOKEN_RESPONSE | grep -o '"expires_in":[0-9]*' | cut -d':' -f2)

echo "  Token Type: $TOKEN_TYPE"
echo "  Expires In: $EXPIRES_IN seconds"
echo "  Refresh Token: ${REFRESH_TOKEN:0:50}..."
echo ""

# Step 6: Decode JWT Token
echo -e "\n${BLUE}Step 6: Decoding JWT Token${NC}"
echo "================================================"

print_status "Decoding JWT payload..."

# Decode JWT (extract payload and decode base64)
JWT_PAYLOAD=$(echo $USER_ACCESS_TOKEN | cut -d'.' -f2)
# Add padding if needed
JWT_PAYLOAD_PADDED="${JWT_PAYLOAD}$(printf '%*s' $((4 - ${#JWT_PAYLOAD} % 4)) '' | tr ' ' '=')"
DECODED_PAYLOAD=$(echo $JWT_PAYLOAD_PADDED | base64 -d 2>/dev/null || echo $JWT_PAYLOAD_PADDED | base64 -D 2>/dev/null)

echo "JWT Payload:"
echo "$DECODED_PAYLOAD" | python3 -m json.tool 2>/dev/null || echo "$DECODED_PAYLOAD"
echo ""

# Step 7: Test Auth-Service Integration
echo -e "\n${BLUE}Step 7: Testing Auth-Service Integration${NC}"
echo "================================================"

print_status "Checking if auth-service is running..."

if curl -s -f "${AUTH_SERVICE_URL}/actuator/health" > /dev/null 2>&1; then
    print_success "Auth-service is running"
    
    print_status "Testing Keycloak token with auth-service..."
    
    # Test /api/auth/me endpoint
    ME_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${AUTH_SERVICE_URL}/api/auth/me" \
      -H "Authorization: Bearer ${USER_ACCESS_TOKEN}" \
      -H "Content-Type: application/json")
    
    HTTP_CODE=$(echo "$ME_RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
    RESPONSE_BODY=$(echo "$ME_RESPONSE" | sed '/HTTP_CODE:/d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success "Keycloak token validated successfully by auth-service"
        echo "User Info Response:"
        echo "$RESPONSE_BODY" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE_BODY"
    else
        print_warning "Auth-service returned HTTP $HTTP_CODE"
        echo "Response: $RESPONSE_BODY"
    fi
    
    echo ""
    
    # Test /api/auth/authorities endpoint
    print_status "Testing authorities endpoint..."
    AUTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${AUTH_SERVICE_URL}/api/auth/authorities" \
      -H "Authorization: Bearer ${USER_ACCESS_TOKEN}" \
      -H "Content-Type: application/json")
    
    HTTP_CODE=$(echo "$AUTH_RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
    RESPONSE_BODY=$(echo "$AUTH_RESPONSE" | sed '/HTTP_CODE:/d')
    
    if [ "$HTTP_CODE" = "200" ]; then
        print_success "Authorities retrieved successfully"
        echo "Authorities Response:"
        echo "$RESPONSE_BODY" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE_BODY"
    else
        print_warning "Authorities endpoint returned HTTP $HTTP_CODE"
        echo "Response: $RESPONSE_BODY"
    fi
    
else
    print_warning "Auth-service is not running"
    print_status "Starting auth-service in Keycloak mode..."
    echo ""
    echo "Run the following command in a separate terminal:"
    echo "cd /Users/duongphamthaibinh/Downloads/SourceCode/design/beautiful/java/microservices/auth-service"
    echo "./scripts/start-keycloak.sh"
    echo ""
    echo "Or start in dual mode (supports both Custom JWT and Keycloak):"
    echo "./scripts/start-dual.sh"
fi

echo ""

# Step 8: Test Token Introspection
echo -e "\n${BLUE}Step 8: Testing Token Introspection${NC}"
echo "================================================"

print_status "Introspecting token..."

INTROSPECT_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token/introspect" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" \
  -d "token=${USER_ACCESS_TOKEN}")

echo "Token Introspection Response:"
echo "$INTROSPECT_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$INTROSPECT_RESPONSE"
echo ""

# Step 9: Test Token Refresh
echo -e "\n${BLUE}Step 9: Testing Token Refresh${NC}"
echo "================================================"

if [ -n "$REFRESH_TOKEN" ]; then
    print_status "Refreshing access token..."
    
    REFRESH_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=${CLIENT_ID}" \
      -d "client_secret=${CLIENT_SECRET}" \
      -d "grant_type=refresh_token" \
      -d "refresh_token=${REFRESH_TOKEN}")
    
    NEW_ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$NEW_ACCESS_TOKEN" ]; then
        print_success "Token refreshed successfully"
        echo "New Access Token (first 50 chars): ${NEW_ACCESS_TOKEN:0:50}..."
    else
        print_error "Token refresh failed"
        echo "Response: $REFRESH_RESPONSE"
    fi
else
    print_warning "No refresh token available"
fi

echo ""

# Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}📊 Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
print_success "Keycloak is running at: ${KEYCLOAK_URL}"
print_success "Admin Console: ${KEYCLOAK_URL}/admin"
print_success "Admin Credentials: ${ADMIN_USERNAME} / ${ADMIN_PASSWORD}"
echo ""
print_success "Test User Credentials:"
echo "  Username: ${TEST_USERNAME}"
echo "  Password: ${TEST_PASSWORD}"
echo ""
print_success "Client Configuration:"
echo "  Client ID: ${CLIENT_ID}"
echo "  Client Secret: ${CLIENT_SECRET}"
echo "  Realm: ${KEYCLOAK_REALM}"
echo ""

if curl -s -f "${AUTH_SERVICE_URL}/actuator/health" > /dev/null 2>&1; then
    print_success "Auth-Service is running at: ${AUTH_SERVICE_URL}"
    print_success "Keycloak integration is working!"
else
    print_warning "Auth-Service is not running"
    echo ""
    echo "To start auth-service with Keycloak support:"
    echo "  ./scripts/start-keycloak.sh    # Keycloak only"
    echo "  ./scripts/start-dual.sh        # Both Custom JWT and Keycloak"
fi

echo ""
echo -e "${GREEN}✅ Keycloak test completed!${NC}"
echo ""

# Useful commands
echo -e "${BLUE}Useful Commands:${NC}"
echo "================================================"
echo "# Check Keycloak logs:"
echo "docker-compose logs -f keycloak"
echo ""
echo "# Stop Keycloak:"
echo "docker-compose stop keycloak"
echo ""
echo "# Restart Keycloak:"
echo "docker-compose restart keycloak"
echo ""
echo "# Clean up and restart:"
echo "docker-compose down && docker-compose up -d"
echo ""

