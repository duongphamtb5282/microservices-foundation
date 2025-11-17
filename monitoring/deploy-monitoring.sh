#!/bin/bash

# Microservices Monitoring Stack Deployment Script
# Deploys Prometheus and AlertManager to EKS

set -e

NAMESPACE="monitoring"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi

    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI is not installed. Please install AWS CLI first."
        exit 1
    fi

    if ! kubectl cluster-info &> /dev/null; then
        log_error "Unable to connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi

    log_success "Prerequisites check passed"
}

# Create namespace
create_namespace() {
    log_info "Creating monitoring namespace..."
    kubectl apply -f "$SCRIPT_DIR/k8s/namespace.yml"
    log_success "Namespace created"
}

# Deploy RBAC
deploy_rbac() {
    log_info "Deploying RBAC resources..."
    kubectl apply -f "$SCRIPT_DIR/k8s/prometheus-rbac.yml"
    kubectl apply -f "$SCRIPT_DIR/k8s/alertmanager-rbac.yml"
    log_success "RBAC resources deployed"
}

# Deploy ConfigMaps
deploy_configmaps() {
    log_info "Deploying ConfigMaps..."
    kubectl apply -f "$SCRIPT_DIR/k8s/prometheus-configmap.yml"
    kubectl apply -f "$SCRIPT_DIR/k8s/prometheus-alerts-configmap.yml"
    kubectl apply -f "$SCRIPT_DIR/k8s/alertmanager-configmap.yml"
    log_success "ConfigMaps deployed"
}

# Deploy Storage
deploy_storage() {
    log_info "Deploying persistent storage..."
    kubectl apply -f "$SCRIPT_DIR/k8s/prometheus-pvc.yml"
    kubectl apply -f "$SCRIPT_DIR/k8s/alertmanager-pvc.yml"
    log_success "Persistent storage deployed"
}

# Deploy Services
deploy_services() {
    log_info "Deploying services..."
    kubectl apply -f "$SCRIPT_DIR/k8s/prometheus-service.yml"
    kubectl apply -f "$SCRIPT_DIR/k8s/alertmanager-service.yml"
    log_success "Services deployed"
}

# Deploy Applications
deploy_applications() {
    log_info "Deploying applications..."

    # Deploy Prometheus
    log_info "Deploying Prometheus..."
    kubectl apply -f "$SCRIPT_DIR/k8s/prometheus-deployment.yml"

    # Wait for Prometheus to be ready
    log_info "Waiting for Prometheus to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/prometheus -n monitoring

    # Deploy AlertManager
    log_info "Deploying AlertManager..."
    kubectl apply -f "$SCRIPT_DIR/k8s/alertmanager-deployment.yml"

    # Wait for AlertManager to be ready
    log_info "Waiting for AlertManager to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/alertmanager -n monitoring

    log_success "Applications deployed"
}

# Setup port forwarding for local access
setup_port_forwarding() {
    log_info "Setting up port forwarding for local access..."
    log_warn "Note: Port forwarding will run in background. Press Ctrl+C to stop when done."

    # Prometheus
    kubectl port-forward -n monitoring svc/prometheus 9090:9090 &
    PROMETHEUS_PID=$!

    # AlertManager
    kubectl port-forward -n monitoring svc/alertmanager 9093:9093 &
    ALERTMANAGER_PID=$!

    log_success "Port forwarding established:"
    log_success "  Prometheus: http://localhost:9090"
    log_success "  AlertManager: http://localhost:9093"

    # Wait for user input
    echo ""
    log_info "Press Enter to stop port forwarding and continue..."
    read -r

    # Kill background processes
    kill $PROMETHEUS_PID $ALERTMANAGER_PID 2>/dev/null || true
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."

    # Check pods
    log_info "Checking pod status..."
    kubectl get pods -n monitoring

    # Check services
    log_info "Checking service status..."
    kubectl get svc -n monitoring

    # Check PVCs
    log_info "Checking persistent volume claims..."
    kubectl get pvc -n monitoring

    # Test Prometheus
    if kubectl exec -n monitoring deployment/prometheus -- curl -s http://localhost:9090/-/healthy > /dev/null; then
        log_success "Prometheus health check passed"
    else
        log_error "Prometheus health check failed"
    fi

    # Test AlertManager
    if kubectl exec -n monitoring deployment/alertmanager -- curl -s http://localhost:9093/-/healthy > /dev/null; then
        log_success "AlertManager health check passed"
    else
        log_error "AlertManager health check failed"
    fi
}

# Show access information
show_access_info() {
    log_success "Monitoring stack deployed successfully!"
    echo ""
    log_info "Access Information:"
    echo "  Prometheus: kubectl port-forward -n monitoring svc/prometheus 9090:9090"
    echo "             URL: http://localhost:9090"
    echo "  AlertManager: kubectl port-forward -n monitoring svc/alertmanager 9093:9093"
    echo "                URL: http://localhost:9093"
    echo ""
    log_info "To access from outside the cluster, consider setting up Ingress or LoadBalancer services."
    log_info "Make sure your microservices have the required annotations for Prometheus scraping."
}

# Main deployment function
main() {
    echo "=========================================="
    echo "  Microservices Monitoring Stack Deployment"
    echo "=========================================="
    echo ""

    check_prerequisites

    create_namespace
    deploy_rbac
    deploy_configmaps
    deploy_storage
    deploy_services
    deploy_applications

    verify_deployment

    echo ""
    read -p "Would you like to set up port forwarding for local access? (y/N): " -r
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        setup_port_forwarding
    fi

    show_access_info

    log_success "Deployment completed successfully!"
}

# Handle command line arguments
case "${1:-}" in
    "verify")
        verify_deployment
        ;;
    "cleanup")
        log_info "Cleaning up monitoring stack..."
        kubectl delete namespace monitoring --ignore-not-found=true
        log_success "Cleanup completed"
        ;;
    *)
        main
        ;;
esac
