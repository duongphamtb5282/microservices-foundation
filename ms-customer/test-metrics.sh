#!/bin/bash
# Test script to verify metrics collection is working

NAMESPACE="customer-service"
SERVICE_NAME="ms-customer"

echo "🧪 Testing MS-Customer Metrics Collection..."

# Check if pod is running
echo "1. Checking pod status..."
kubectl get pods -n $NAMESPACE -l app=$SERVICE_NAME
if [ $? -ne 0 ]; then
    echo "❌ No pods found. Deployment may have failed."
    exit 1
fi

# Get pod name
POD_NAME=$(kubectl get pods -n $NAMESPACE -l app=$SERVICE_NAME -o jsonpath='{.items[0].metadata.name}')
echo "📦 Using pod: $POD_NAME"

# Test health endpoint
echo ""
echo "2. Testing health endpoint..."
HEALTH=$(kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8084/actuator/health)
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo "✅ Health check passed"
else
    echo "❌ Health check failed: $HEALTH"
    exit 1
fi

# Test metrics endpoint
echo ""
echo "3. Testing metrics endpoint..."
METRICS=$(kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8084/actuator/metrics)
if echo "$METRICS" | grep -q '"names":'; then
    echo "✅ Metrics endpoint accessible"
else
    echo "❌ Metrics endpoint failed"
    exit 1
fi

# Test Prometheus metrics
echo ""
echo "4. Testing Prometheus metrics..."
PROMETHEUS_METRICS=$(kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8084/actuator/prometheus)

# Check for JVM metrics
JVM_COUNT=$(echo "$PROMETHEUS_METRICS" | grep -c "^jvm_")
if [ $JVM_COUNT -gt 0 ]; then
    echo "✅ JVM metrics found: $JVM_COUNT metrics"
    echo "$PROMETHEUS_METRICS" | grep "^jvm_" | head -3
else
    echo "❌ No JVM metrics found"
fi

# Check for HTTP metrics
HTTP_COUNT=$(echo "$PROMETHEUS_METRICS" | grep -c "^http_server_requests_seconds")
if [ $HTTP_COUNT -gt 0 ]; then
    echo "✅ HTTP metrics found: $HTTP_COUNT metrics"
    echo "$PROMETHEUS_METRICS" | grep "^http_server_requests_seconds" | head -2
else
    echo "❌ No HTTP metrics found"
fi

# Check for MongoDB driver metrics (from Spring Boot)
MONGODB_DRIVER_COUNT=$(echo "$PROMETHEUS_METRICS" | grep -c "^mongodb_driver_pool")
if [ $MONGODB_DRIVER_COUNT -gt 0 ]; then
    echo "✅ MongoDB driver metrics found: $MONGODB_DRIVER_COUNT metrics"
    echo "$PROMETHEUS_METRICS" | grep "^mongodb_driver_pool" | head -2
else
    echo "⚠️  No MongoDB driver metrics found (may not have DB operations yet)"
fi

# Test with a sample request to generate metrics
echo ""
echo "5. Generating sample traffic..."
kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8084/actuator/health > /dev/null
kubectl exec -n $NAMESPACE $POD_NAME -- curl -s http://localhost:8084/actuator/metrics > /dev/null

# Check if Prometheus can scrape
echo ""
echo "6. Checking Prometheus scraping (if available)..."
if kubectl get svc prometheus -n monitoring &> /dev/null; then
    echo "📊 Prometheus found, checking targets..."
    # This would require port-forwarding in a real scenario
    echo "💡 To check Prometheus targets manually:"
    echo "   kubectl port-forward svc/prometheus 9090:9090 -n monitoring"
    echo "   Visit: http://localhost:9090/targets"
else
    echo "⚠️  Prometheus not found in monitoring namespace"
fi

# Summary
echo ""
echo "🎉 Metrics test completed!"
echo ""
echo "📊 Summary:"
echo "- Health endpoint: ✅ Working"
echo "- Metrics endpoint: ✅ Working"
echo "- JVM metrics: $JVM_COUNT found"
echo "- HTTP metrics: $HTTP_COUNT found"
echo ""
echo "🔍 Next steps:"
echo "1. Access metrics: kubectl port-forward svc/$SERVICE_NAME 8084:8084 -n $NAMESPACE"
echo "2. View Prometheus: kubectl port-forward svc/prometheus 9090:9090 -n monitoring"
echo "3. Check Grafana: kubectl port-forward svc/grafana 3000:3000 -n monitoring"
echo ""
echo "📈 Key metrics to monitor:"
echo "- JVM heap usage: jvm_memory_used_bytes{area=\"heap\"}"
echo "- HTTP error rate: http_server_requests_seconds_count{status=~\"5..\"}"
echo "- CPU usage: container_cpu_usage_seconds_total"
echo "- Response time: histogram_quantile(0.95, http_server_requests_seconds_bucket)"
echo "- MongoDB connections: mongodb_connections{state=\"current\"}"
echo "- DB memory: mongodb_memory{type=\"resident\"}"
