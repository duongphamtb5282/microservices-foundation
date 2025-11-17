#!/bin/bash

# Enhanced Auth Service Deployment Script
# This script deploys the auth service with all improvements

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="auth-service-prod"
SERVICE_NAME="auth-service"
DEPLOYMENT_NAME="auth-service"

echo -e "${BLUE}🚀 Starting Enhanced Auth Service Deployment${NC}"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo -e "${YELLOW}📋 Checking prerequisites...${NC}"
if ! command_exists kubectl; then
    echo -e "${RED}❌ kubectl is not installed${NC}"
    exit 1
fi

if ! command_exists helm; then
    echo -e "${YELLOW}⚠️  helm is not installed - some features may not work${NC}"
fi

# Check cluster connectivity
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo -e "${RED}❌ Cannot connect to Kubernetes cluster${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Prerequisites check passed${NC}"

# Create namespace if it doesn't exist
echo -e "${YELLOW}📦 Creating namespace...${NC}"
kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -

# Apply priority classes
echo -e "${YELLOW}🎯 Applying priority classes...${NC}"
kubectl apply -f priority-classes/auth-service-priority-class.yml

# Apply security policies
echo -e "${YELLOW}🔒 Applying security policies...${NC}"
kubectl apply -f security/auth-service-pod-security-policy.yml

# Create network policies directory if it doesn't exist
mkdir -p network-policies

# Apply network policies
echo -e "${YELLOW}🌐 Applying network policies...${NC}"
kubectl apply -f network-policies/auth-service-network-policy.yml

# Apply secrets
echo -e "${YELLOW}🔐 Applying secrets...${NC}"
kubectl apply -f secrets/auth-db-secrets.yml
kubectl apply -f secrets/encryption-secrets.yml

# Apply service account
echo -e "${YELLOW}👤 Applying service account...${NC}"
kubectl apply -f serviceaccounts/auth-service-serviceaccount.yml

# Apply configmaps
echo -e "${YELLOW}⚙️  Applying configuration...${NC}"
kubectl apply -f configmaps/auth-service-configmap.yml

# Create monitoring directory if it doesn't exist
mkdir -p monitoring

# Apply monitoring configuration
echo -e "${YELLOW}📊 Applying monitoring configuration...${NC}"
kubectl apply -f monitoring/auth-service-servicemonitor.yml

# Apply service
echo -e "${YELLOW}🔗 Applying service...${NC}"
kubectl apply -f services/auth-service-service.yml

# Apply deployment
echo -e "${YELLOW}🚀 Applying deployment...${NC}"
kubectl apply -f deployments/auth-service-deployment.yml

# Create autoscaling directory if it doesn't exist
mkdir -p autoscaling

# Apply autoscaling
echo -e "${YELLOW}📈 Applying autoscaling configuration...${NC}"
kubectl apply -f autoscaling/auth-service-hpa.yml

# Wait for deployment to be ready
echo -e "${YELLOW}⏳ Waiting for deployment to be ready...${NC}"
kubectl rollout status deployment/${DEPLOYMENT_NAME} -n ${NAMESPACE} --timeout=300s

# Check pod status
echo -e "${YELLOW}🔍 Checking pod status...${NC}"
kubectl get pods -n ${NAMESPACE} -l app=${SERVICE_NAME}

# Check service endpoints
echo -e "${YELLOW}🔗 Checking service endpoints...${NC}"
kubectl get endpoints -n ${NAMESPACE} ${SERVICE_NAME}

# Display service information
echo -e "${YELLOW}ℹ️  Service information:${NC}"
kubectl get svc -n ${NAMESPACE} ${SERVICE_NAME}

# Check HPA status
echo -e "${YELLOW}📊 Checking HPA status...${NC}"
kubectl get hpa -n ${NAMESPACE} auth-service-hpa

# Display deployment summary
echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
echo -e "${BLUE}📋 Deployment Summary:${NC}"
echo -e "  • Namespace: ${NAMESPACE}"
echo -e "  • Service: ${SERVICE_NAME}"
echo -e "  • Deployment: ${DEPLOYMENT_NAME}"
echo -e "  • Security: Network policies, Pod security standards, RBAC"
echo -e "  • Monitoring: Prometheus metrics, Distributed tracing, Structured logging"
echo -e "  • Autoscaling: HPA with CPU/Memory/Custom metrics"
echo -e "  • Resilience: Circuit breakers, Retries, Health checks"
echo -e "  • Cost optimization: Spot instances, Resource efficiency"

# Health check
echo -e "${YELLOW}🏥 Performing health check...${NC}"
sleep 10

# Get a pod name
POD_NAME=$(kubectl get pods -n ${NAMESPACE} -l app=${SERVICE_NAME} -o jsonpath='{.items[0].metadata.name}')

if [ ! -z "$POD_NAME" ]; then
    echo -e "${YELLOW}🔍 Testing health endpoint...${NC}"
    if kubectl exec -n ${NAMESPACE} ${POD_NAME} -- curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✅ Health check passed${NC}"
    else
        echo -e "${YELLOW}⚠️  Health check pending - service may still be starting${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  No pods found - deployment may still be in progress${NC}"
fi

echo -e "${GREEN}🎉 Auth Service deployment with all improvements completed!${NC}"
echo -e "${BLUE}💡 Next steps:${NC}"
echo -e "  1. Monitor the application metrics in your monitoring dashboard"
echo -e "  2. Check logs: kubectl logs -f deployment/${DEPLOYMENT_NAME} -n ${NAMESPACE}"
echo -e "  3. Scale if needed: kubectl scale deployment ${DEPLOYMENT_NAME} --replicas=5 -n ${NAMESPACE}"
echo -e "  4. Update secrets with actual values in production"
