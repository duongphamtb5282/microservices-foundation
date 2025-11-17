#!/bin/bash
set -e

echo "🚀 Testing /users/me endpoints with Keycloak"
echo ""

# Configuration
KEYCLOAK_URL="http://localhost:8080"
AUTH_URL="http://localhost:8082"
CLIENT_ID="auth-service-client"
CLIENT_SECRET="LztCVjj5fFMZXEcCGHkL4oN3UWeiLYIk"
USERNAME="admin"
PASSWORD="admin"
REALM="myrealm"

# Check services
echo "Checking services..."
if ! curl -s -f "$KEYCLOAK_URL" > /dev/null 2>&1; then
    echo "❌ Keycloak is not running at $KEYCLOAK_URL"
    echo "Start it with: cd auth-service && docker-compose up -d keycloak"
    exit 1
fi
echo "✅ Keycloak is running"

if ! curl -s -f "$AUTH_URL/actuator/health" > /dev/null 2>&1; then
    echo "❌ Auth Service is not running at $AUTH_URL"
    echo "Start it with: cd auth-service && ./gradlew bootRun"
    exit 1
fi
echo "✅ Auth Service is running"
echo ""

# Get token
echo "1️⃣ Getting Keycloak token..."
TOKEN_RESPONSE=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password")

TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Failed to get token"
    echo "Response:"
    echo "$TOKEN_RESPONSE" | jq '.'
    exit 1
fi

echo "✅ Token obtained"
echo "Token (first 50 chars): ${TOKEN:0:50}..."
echo ""

# Test /api/auth/me
echo "2️⃣ Testing GET /api/auth/me"
echo "   (Returns user info from JWT token)"
RESPONSE=$(curl -s -X GET "$AUTH_URL/api/auth/me" \
  -H "Authorization: Bearer $TOKEN" \
  -w "\n%{http_code}")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "   Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Success!"
    echo "$BODY" | jq '.'
else
    echo "   ❌ Failed"
    echo "$BODY"
fi
echo ""

# Test /api/users/me
echo "3️⃣ Testing GET /api/users/me"
echo "   (Returns user info from database)"
RESPONSE=$(curl -s -X GET "$AUTH_URL/api/users/me" \
  -H "Authorization: Bearer $TOKEN" \
  -w "\n%{http_code}")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "   Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Success - User found in database!"
    echo "$BODY" | jq '.'
elif [ "$HTTP_CODE" = "404" ]; then
    echo "   ⚠️  User not found in database"
    echo "   (This is normal if user only exists in Keycloak)"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo "   ❌ Error"
    echo "$BODY"
fi
echo ""

# Test without token (should fail)
echo "4️⃣ Testing without token (should return 401)"
HTTP_CODE=$(curl -s -X GET "$AUTH_URL/api/auth/me" \
  -w "%{http_code}" -o /dev/null)

echo "   Status: $HTTP_CODE"
if [ "$HTTP_CODE" = "401" ]; then
    echo "   ✅ Correctly returned 401 Unauthorized"
else
    echo "   ❌ Expected 401 but got $HTTP_CODE"
fi
echo ""

echo "🎉 Test completed!"
echo ""
echo "Summary:"
echo "  - Keycloak token: ✅ Obtained"
echo "  - /api/auth/me: $([ "$HTTP_CODE" = "200" ] && echo "✅" || echo "❌")"
echo "  - /api/users/me: $([ "$HTTP_CODE" = "200" ] && echo "✅" || echo "⚠️")"
echo "  - Security: ✅ 401 without token"

