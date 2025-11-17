#!/bin/bash

# EKS Deployment Script for Microservices
# This script deploys all microservices to EKS in the correct order

set -e

echo "🚀 Starting EKS deployment of microservices..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Check if kubectl is configured
if ! kubectl cluster-info >/dev/null 2>&1; then
    print_error "kubectl is not configured or cluster is not accessible"
    exit 1
fi

print_status "kubectl is configured and cluster is accessible"

# Check if AWS CLI is configured
if ! aws sts get-caller-identity >/dev/null 2>&1; then
    print_error "AWS CLI is not configured"
    exit 1
fi

print_status "AWS CLI is configured"

# Function to deploy resources with error handling
deploy_resources() {
    local dir=$1
    local description=$2

    if [ -d "$dir" ]; then
        print_status "Deploying $description..."
        for file in "$dir"/*.yml; do
            if [ -f "$file" ]; then
                print_status "Applying $file..."
                if kubectl apply -f "$file"; then
                    print_success "Successfully applied $file"
                else
                    print_error "Failed to apply $file"
                    exit 1
                fi
            fi
        done
        print_success "All $description deployed successfully"
    else
        print_warning "Directory $dir not found, skipping..."
    fi
}

# Deploy in order
print_status "Starting deployment sequence..."

deploy_resources "namespaces" "namespaces"
deploy_resources "serviceaccounts" "service accounts"
deploy_resources "secrets" "secrets"
deploy_resources "configmaps" "configmaps"
deploy_resources "services" "services"
deploy_resources "deployments" "deployments"

print_success "🎉 All microservices deployed successfully!"

# Wait for deployments to be ready
print_status "Waiting for deployments to be ready..."

kubectl wait --for=condition=available --timeout=300s deployment/api-gateway -n api-gateway-prod
kubectl wait --for=condition=available --timeout=300s deployment/auth-service -n auth-service-prod
kubectl wait --for=condition=available --timeout=300s deployment/ms-customer -n ms-customer-prod
kubectl wait --for=condition=available --timeout=300s deployment/ms-order-service -n ms-order-service-prod
kubectl wait --for=condition=available --timeout=300s deployment/ms-payment-service -n ms-payment-service-prod

print_success "All deployments are ready!"

# Display service endpoints
print_status "Service endpoints:"
echo ""
echo "API Gateway (External):"
kubectl get svc api-gateway -n api-gateway-prod -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "LoadBalancer not ready yet"
echo ""
echo "Internal Services:"
echo "Auth Service:     http://auth-service.auth-service-prod.svc.cluster.local:8080"
echo "Customer Service: http://ms-customer.ms-customer-prod.svc.cluster.local:8084"
echo "Order Service:    http://ms-order-service.ms-order-service-prod.svc.cluster.local:8081"
echo "Payment Service:  http://ms-payment-service.ms-payment-service-prod.svc.cluster.local:8080"

print_success "Deployment completed successfully! 🚀"
