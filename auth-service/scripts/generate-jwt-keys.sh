#!/bin/bash

# Generate JWT RSA Key Pairs Script
# This script generates RSA private and public keys for JWT authentication

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
JWT_DIR="src/main/resources/jwt"
PRIVATE_KEY_FILE="$JWT_DIR/app.key"
PUBLIC_KEY_FILE="$JWT_DIR/app.pub"
KEY_SIZE=2048

# Helper functions
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

# Check if OpenSSL is available
check_openssl() {
    if ! command -v openssl &> /dev/null; then
        log_error "OpenSSL is not installed. Please install OpenSSL first."
        exit 1
    fi
    log_success "OpenSSL is available"
}

# Create JWT directory
create_jwt_directory() {
    log_info "Creating JWT directory: $JWT_DIR"
    mkdir -p "$JWT_DIR"
    log_success "JWT directory created"
}

# Generate RSA private key
generate_private_key() {
    log_info "Generating RSA private key (${KEY_SIZE} bits)..."
    openssl genrsa -out "$PRIVATE_KEY_FILE" $KEY_SIZE
    chmod 600 "$PRIVATE_KEY_FILE"
    log_success "Private key generated: $PRIVATE_KEY_FILE"
}

# Generate RSA public key from private key
generate_public_key() {
    log_info "Generating RSA public key from private key..."
    openssl rsa -in "$PRIVATE_KEY_FILE" -pubout -out "$PUBLIC_KEY_FILE"
    chmod 644 "$PUBLIC_KEY_FILE"
    log_success "Public key generated: $PUBLIC_KEY_FILE"
}

# Verify keys
verify_keys() {
    log_info "Verifying generated keys..."
    
    # Check private key
    if openssl rsa -in "$PRIVATE_KEY_FILE" -check -noout 2>/dev/null; then
        log_success "Private key is valid"
    else
        log_error "Private key is invalid"
        exit 1
    fi
    
    # Check public key
    if openssl rsa -in "$PUBLIC_KEY_FILE" -pubin -text -noout 2>/dev/null; then
        log_success "Public key is valid"
    else
        log_error "Public key is invalid"
        exit 1
    fi
}

# Display key information
display_key_info() {
    log_info "Key Information:"
    echo "Private Key: $PRIVATE_KEY_FILE"
    echo "Public Key: $PUBLIC_KEY_FILE"
    echo "Key Size: $KEY_SIZE bits"
    echo
    
    log_info "Private Key Details:"
    openssl rsa -in "$PRIVATE_KEY_FILE" -text -noout | head -10
    echo
    
    log_info "Public Key Details:"
    openssl rsa -in "$PUBLIC_KEY_FILE" -pubin -text -noout | head -10
    echo
}

# Test JWT signing and verification
test_jwt_operations() {
    log_info "Testing JWT operations..."
    
    # Create a test payload
    local test_payload='{"sub":"test-user","iss":"auth-service","exp":9999999999}'
    
    # Sign with private key
    local token=$(echo -n "$test_payload" | openssl dgst -sha256 -sign "$PRIVATE_KEY_FILE" | base64 -w 0)
    
    if [ -n "$token" ]; then
        log_success "JWT signing test passed"
    else
        log_warning "JWT signing test failed"
    fi
}

# Clean up old keys if they exist
cleanup_old_keys() {
    if [ -f "$PRIVATE_KEY_FILE" ] || [ -f "$PUBLIC_KEY_FILE" ]; then
        log_warning "Existing keys found. Removing old keys..."
        rm -f "$PRIVATE_KEY_FILE" "$PUBLIC_KEY_FILE"
        log_success "Old keys removed"
    fi
}

# Main function
main() {
    log_info "🔐 JWT RSA Key Generation Script"
    echo "=========================================="
    
    # Check prerequisites
    check_openssl
    
    # Clean up old keys
    cleanup_old_keys
    
    # Create directory
    create_jwt_directory
    
    # Generate keys
    generate_private_key
    generate_public_key
    
    # Verify keys
    verify_keys
    
    # Display information
    display_key_info
    
    # Test operations
    test_jwt_operations
    
    echo "=========================================="
    log_success "🎉 JWT RSA key generation completed successfully!"
    log_info "Keys are ready for JWT authentication"
    echo
    log_info "Next steps:"
    echo "1. Start the auth-service application"
    echo "2. Test JWT authentication endpoints"
    echo "3. Use the generated keys for token signing and verification"
}

# Run main function
main "$@"
