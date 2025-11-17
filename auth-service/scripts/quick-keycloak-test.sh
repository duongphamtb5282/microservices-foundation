#!/bin/bash

# Quick Keycloak Integration Test
# Tests Keycloak token with running auth-service

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

KEYCLOAK_URL="http://localhost:8080"
AUTH_SERVICE_URL="http://localhost:8082"
REALM="auth-service"
CLIENT_ID="auth-service-client"
CLIENT_SECRET="your-client-secret-here"
USERNAME="testuser"
PASSWORD="password123"

echo -e "${BLUE}🔐 Quick Keycloak Integration Test${NC}\n"

# Step 1: Get Keycloak Token
echo -e "${BLUE}Step 1: Getting Keycloak Token${NC}"
echo "================================================"

TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${CLIENT_ID}" \
  -d "client_secret=${CLIENT_SECRET}" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}" \
  -d "grant_type=password")

ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}✗ Failed to get access token${NC}"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Token obtained successfully${NC}"
echo "Token (first 50 chars): ${ACCESS_TOKEN:0:50}..."
echo ""

# Step 2: Test with Auth-Service
echo -e "${BLUE}Step 2: Testing with Auth-Service${NC}"
echo "================================================"

# Test /api/auth/me
echo "Testing /api/auth/me endpoint..."
ME_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${AUTH_SERVICE_URL}/api/auth/me" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$ME_RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
RESPONSE_BODY=$(echo "$ME_RESPONSE" | sed '/HTTP_CODE:/d')

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Success!${NC}"
    echo "Response:"
    echo "$RESPONSE_BODY" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE_BODY"
else
    echo -e "${RED}✗ Failed${NC}"
    echo "Response: $RESPONSE_BODY"
fi
echo ""

# Test /api/auth/authorities
echo "Testing /api/auth/authorities endpoint..."
AUTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${AUTH_SERVICE_URL}/api/auth/authorities" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$AUTH_RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
RESPONSE_BODY=$(echo "$AUTH_RESPONSE" | sed '/HTTP_CODE:/d')

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Success!${NC}"
    echo "Response:"
    echo "$RESPONSE_BODY" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE_BODY"
else
    echo -e "${RED}✗ Failed${NC}"
    echo "Response: $RESPONSE_BODY"
fi
echo ""

# Step 3: Test Protected Endpoint
echo -e "${BLUE}Step 3: Testing Protected User Endpoint${NC}"
echo "================================================"

USER_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${AUTH_SERVICE_URL}/api/users" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$USER_RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
RESPONSE_BODY=$(echo "$USER_RESPONSE" | sed '/HTTP_CODE:/d')

echo "HTTP Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Success! Keycloak authentication works!${NC}"
    echo "Response:"
    echo "$RESPONSE_BODY" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE_BODY"
else
    echo -e "${RED}✗ Failed${NC}"
    echo "Response: $RESPONSE_BODY"
fi
echo ""

echo -e "${GREEN}✅ Keycloak Integration Test Complete!${NC}"

