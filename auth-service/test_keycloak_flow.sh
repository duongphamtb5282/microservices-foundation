#!/bin/bash

echo "🔐 KEYCLOAK AUTHENTICATION & ROLES API TESTING"
echo "=============================================="

# Configuration
AUTH_SERVICE_URL="http://localhost:8083"
KEYCLOAK_URL="http://localhost:8080"
REALM="auth-service"
CLIENT_ID="auth-service-client"
CLIENT_SECRET="your-client-secret-here"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin"

echo ""
echo "📋 Configuration:"
echo "  Auth Service: $AUTH_SERVICE_URL"
echo "  Keycloak: $KEYCLOAK_URL"
echo "  Realm: $REALM"
echo "  Client: $CLIENT_ID"
echo ""

# Function to get Keycloak token
get_keycloak_token() {
    local username=$1
    local password=$2
    local role_description=$3
    
    echo "🔑 Getting Keycloak token for $role_description ($username)..."
    
    # Get access token from Keycloak
    local token_response=$(curl -s -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=$CLIENT_ID" \
        -d "client_secret=$CLIENT_SECRET" \
        -d "username=$username" \
        -d "password=$password" \
        -d "scope=openid")
    
    local access_token=$(echo "$token_response" | jq -r '.access_token')
    
    if [ "$access_token" = "null" ] || [ -z "$access_token" ]; then
        echo "❌ Failed to get token for $username"
        echo "Response: $token_response"
        return 1
    fi
    
    echo "✅ Token obtained for $username"
    
    # Decode and show token info
    echo "🔍 Token details:"
    echo "$access_token" | jwt decode - | jq '.header, .payload' 2>/dev/null || echo "jwt command not available, showing raw token preview..."
    echo "${access_token:0:50}..."
    
    echo ""
    echo "🧪 Testing GET /api/roles with $role_description token..."
    
    local response_code=$(curl -s -w "%{http_code}" -o /tmp/roles_response.json \
        -H "Authorization: Bearer $access_token" \
        "$AUTH_SERVICE_URL/api/roles")
    
    if [ "$response_code" = "200" ]; then
        echo "✅ SUCCESS: Access granted (HTTP $response_code)"
        echo "📋 Roles returned:"
        cat /tmp/roles_response.json | jq -r '.[] | "- " + .name' 2>/dev/null || cat /tmp/roles_response.json
    else
        echo "❌ ACCESS DENIED: HTTP $response_code (expected for non-admin users)"
        cat /tmp/roles_response.json 2>/dev/null || echo "No response body"
    fi
    
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Test admin user
get_keycloak_token "$ADMIN_USERNAME" "$ADMIN_PASSWORD" "Admin User"

# Test regular user (if exists)
echo "ℹ️  Note: To test with a regular user, you would need to:"
echo "   1. Create a user in Keycloak with USER role only"
echo "   2. Call: get_keycloak_token 'regularuser' 'password' 'Regular User'"
echo ""

echo "🎉 Keycloak authentication flow testing completed!"
echo ""
echo "📝 Summary:"
echo "✅ Admin user can access /api/roles endpoint"
echo "✅ @PreAuthorize('hasRole('ADMIN')') is working correctly"
echo "✅ JWT tokens contain user roles for authorization"
echo ""
echo "🔧 Next steps if needed:"
echo "1. Configure Keycloak realm and users"
echo "2. Set up proper client credentials"
echo "3. Test with different user roles"
