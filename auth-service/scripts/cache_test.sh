#!/bin/bash

echo "🧪 Auth-Service Cache Testing Script"
echo "===================================="

# Get authentication token
echo "1. Getting authentication token..."
TOKEN=$(curl -s -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"duong","password":"password123"}' | \
  jq -r '.accessToken')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Failed to get authentication token"
  exit 1
fi

echo "✅ Token obtained: ${TOKEN:0:20}..."

# Test cache health
echo -e "\n2. Testing cache health..."
curl -X GET http://localhost:8082/api/cache/health \
  -H "Authorization: Bearer $TOKEN" \
  -s | jq .

# Test cache statistics
echo -e "\n3. Getting cache statistics..."
curl -X GET http://localhost:8082/api/cache/stats \
  -H "Authorization: Bearer $TOKEN" \
  -s | jq .

# Test cache existence
echo -e "\n4. Checking cache existence..."
curl -X GET http://localhost:8082/api/cache/exists \
  -H "Authorization: Bearer $TOKEN" \
  -s | jq .

# Test cache serialization
echo -e "\n5. Testing cache serialization..."
curl -X POST http://localhost:8082/api/cache/test-serialization \
  -H "Authorization: Bearer $TOKEN" \
  -s | jq .

# Test cache reload
echo -e "\n6. Testing cache reload..."
curl -X POST http://localhost:8082/api/cache/reload/all \
  -H "Authorization: Bearer $TOKEN" \
  -s | jq .

echo -e "\n✅ Cache testing completed!"
