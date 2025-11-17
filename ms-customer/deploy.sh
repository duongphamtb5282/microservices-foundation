#!/bin/bash
# MS-Customer Service Deployment Script for EKS
# This script deploys the service with monitoring integration

set -e

NAMESPACE="customer-service"
MONITORING_NAMESPACE="monitoring"
DATADOG_NAMESPACE="datadog"

echo "🚀 Deploying MS-Customer Service to EKS..."

# Create namespace if it doesn't exist
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Deploy service account and RBAC
echo "📋 Creating service account and RBAC..."
kubectl apply -f k8s/serviceaccount.yml

# Deploy the service
echo "🐳 Deploying MS-Customer service..."
kubectl apply -f k8s/service.yml
kubectl apply -f k8s/deployment.yml

# Wait for deployment to be ready
echo "⏳ Waiting for deployment to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/ms-customer -n $NAMESPACE

# Deploy MongoDB exporter (if monitoring namespace exists)
if kubectl get namespace $MONITORING_NAMESPACE &> /dev/null; then
    echo "🍃 Deploying MongoDB exporter..."
    kubectl apply -f k8s/mongodb-exporter.yml
fi

# Update Prometheus configuration (if monitoring namespace exists)
if kubectl get namespace $MONITORING_NAMESPACE &> /dev/null; then
    echo "📊 Updating Prometheus configuration..."
    kubectl apply -f k8s/prometheus-config.yml
    kubectl apply -f k8s/prometheus-alerts.yml

    # Restart Prometheus to pick up new configuration
    kubectl rollout restart deployment/prometheus -n $MONITORING_NAMESPACE
fi

# Update Datadog configuration (if datadog namespace exists)
if kubectl get namespace $DATADOG_NAMESPACE &> /dev/null; then
    echo "🐶 Updating Datadog configuration..."
    kubectl apply -f k8s/datadog-config.yml

    # Restart Datadog agent to pick up new configuration
    kubectl rollout restart daemonset/datadog-agent -n $DATADOG_NAMESPACE
fi

# Verify deployment
echo "✅ Verifying deployment..."
kubectl get pods -n $NAMESPACE -l app=ms-customer
kubectl get svc -n $NAMESPACE -l app=ms-customer

# Test metrics endpoint
echo "📈 Testing metrics endpoint..."
sleep 10
POD_NAME=$(kubectl get pods -n $NAMESPACE -l app=ms-customer -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8084/actuator/health | head -5
kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8084/actuator/prometheus | grep -E "(jvm_|http_server_)" | head -5

echo "🎉 Deployment completed successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Verify metrics in Prometheus: http://prometheus:9090"
echo "2. Check Datadog dashboard for JVM metrics"
echo "3. Test application endpoints"
echo ""
echo "🔍 Useful commands:"
echo "kubectl logs -f deployment/ms-customer -n $NAMESPACE"
echo "kubectl port-forward svc/ms-customer 8084:8084 -n $NAMESPACE"
echo "kubectl exec -it $POD_NAME -n $NAMESPACE -- curl http://localhost:8084/actuator/prometheus"
