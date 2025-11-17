# Microservices Monitoring Stack

A comprehensive monitoring solution for Java microservices deployed on Amazon EKS using Prometheus and AlertManager.

## 📁 Directory Structure

```
monitoring/
├── k8s/                          # Core monitoring infrastructure
│   ├── namespace.yml             # Monitoring namespace
│   ├── prometheus-*.yml          # Prometheus components
│   ├── alertmanager-*.yml        # AlertManager components
│   └── services/                 # Service-specific monitoring configs
│       ├── ms-customer-datadog-config.yml
│       ├── ms-customer-mongodb-exporter.yml
│       ├── ms-customer-prometheus-alerts.yml
│       ├── ms-customer-prometheus-config.yml
│       ├── ms-order-datadog-config.yml
│       ├── ms-order-postgresql-exporter.yml
│       ├── ms-order-prometheus-alerts.yml
│       ├── ms-order-prometheus-config.yml
│       ├── ms-payment-datadog-config.yml
│       ├── ms-payment-postgresql-exporter.yml
│       ├── ms-payment-prometheus-alerts.yml
│       └── ms-payment-prometheus-config.yml
├── alerts.yml                    # General alerts configuration
├── prometheus.yml                # Prometheus configuration
├── prometheus-queries.md         # Useful Prometheus queries
├── service-annotations.yml       # Service annotation templates
├── deploy-monitoring.sh          # Deployment script
├── docs/                         # Documentation
└── README.md                     # This file
```

## 🚀 Quick Start

```bash
# Deploy the monitoring stack
./deploy-monitoring.sh

# Verify deployment
./deploy-monitoring.sh verify

# Cleanup (if needed)
./deploy-monitoring.sh cleanup
```

## 📋 Table of Contents

- [Architecture](#architecture)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Deployment](#deployment)
- [Configuration](#configuration)
- [Alerting](#alerting)
- [Metrics Collection](#metrics-collection)
- [Accessing the UI](#accessing-the-ui)
- [Troubleshooting](#troubleshooting)
- [Backup & Recovery](#backup--recovery)

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │ Order Service   │    │Payment Service │
│                 │    │                 │    │                │
│ • JVM Metrics   │    │ • JVM Metrics   │    │ • JVM Metrics  │
│ • HTTP Metrics  │    │ • Business KPIs │    │ • Business KPIs│
│ • Circuit Breaker│    │ • Cache Metrics │    │ • Payment Proc │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                     │                      │
          └─────────────────────┼──────────────────────┘
                                │
                   ┌────────────▼────────────┐
                   │                         │
                   │      Prometheus         │
                   │    (Metrics Storage)    │
                   │                         │
                   └────────────┬────────────┘
                                │
                   ┌────────────▼────────────┐
                   │                         │
                   │     AlertManager        │
                   │   (Alert Processing)    │
                   │                         │
                   └─────────────────────────┘
```

## ✨ Features

### Metrics Collection

- **JVM Metrics**: Heap usage, GC statistics, thread counts
- **HTTP Metrics**: Request rates, response times, error rates
- **Business Metrics**: Order processing, payment success rates
- **Infrastructure**: CPU, memory, disk usage
- **Custom Metrics**: Cache hit rates, circuit breaker status

### Alerting

- **Critical Alerts**: Service down, circuit breaker open
- **Warning Alerts**: High CPU/memory, slow responses
- **Business Alerts**: Low payment success, high cancellation rates
- **Security Alerts**: Authentication failures, suspicious activity
- **Multi-channel**: Email notifications with severity-based routing

### Storage & Retention

- **Persistent Storage**: 50GB for Prometheus, 5GB for AlertManager
- **Retention**: 200 hours (8.3 days) of metrics data
- **Compression**: Automatic data compression

## 📋 Prerequisites

### Kubernetes Cluster

- Amazon EKS cluster with version 1.24+
- kubectl configured and authenticated
- AWS CLI installed and configured

### Storage

- EBS CSI driver installed
- `gp3` storage class available
- Sufficient storage quota

### Network

- Cluster can reach external services for email alerts
- Internal DNS resolution working

## 🚀 Deployment

### Automated Deployment

```bash
# Make script executable (first time only)
chmod +x deploy-monitoring.sh

# Deploy everything
./deploy-monitoring.sh
```

### Manual Deployment

If you prefer manual deployment:

```bash
# 1. Create namespace
kubectl apply -f k8s/namespace.yml

# 2. Deploy RBAC
kubectl apply -f k8s/prometheus-rbac.yml
kubectl apply -f k8s/alertmanager-rbac.yml

# 3. Deploy ConfigMaps
kubectl apply -f k8s/prometheus-configmap.yml
kubectl apply -f k8s/prometheus-alerts-configmap.yml
kubectl apply -f k8s/alertmanager-configmap.yml

# 4. Deploy service-specific monitoring (optional)
kubectl apply -f k8s/services/

# Note: Apply only the monitoring configs for services you have deployed

# 5. Deploy storage
kubectl apply -f k8s/prometheus-pvc.yml
kubectl apply -f k8s/alertmanager-pvc.yml

# 6. Deploy services
kubectl apply -f k8s/prometheus-service.yml
kubectl apply -f k8s/alertmanager-service.yml

# 7. Deploy applications
kubectl apply -f k8s/prometheus-deployment.yml
kubectl apply -f k8s/alertmanager-deployment.yml
```

### Verify Deployment

```bash
# Check all resources
kubectl get all -n monitoring

# Check persistent volumes
kubectl get pvc -n monitoring

# Verify Prometheus health
kubectl exec -n monitoring deployment/prometheus -- curl -s http://localhost:9090/-/healthy

# Verify AlertManager health
kubectl exec -n monitoring deployment/alertmanager -- curl -s http://localhost:9093/-/healthy
```

## ⚙️ Configuration

### Prometheus Configuration

The Prometheus configuration is stored in `k8s/prometheus-configmap.yml`:

- **Global Settings**: Scrape interval, evaluation interval
- **Service Discovery**: Kubernetes service and pod discovery
- **Job Configurations**: Custom scrape configs for each service
- **Alerting**: AlertManager integration

### AlertManager Configuration

Email configuration in `k8s/alertmanager-configmap.yml`:

```yaml
global:
  smtp_smarthost: "smtp.gmail.com:587"
  smtp_from: "alerts@yourdomain.com"
  smtp_auth_username: "alerts@yourdomain.com"
  smtp_auth_password: "your-app-password"
```

**Update the email settings before deployment!**

### Alert Rules

Alert rules are defined in `k8s/prometheus-alerts-configmap.yml`:

- **Infrastructure Alerts**: CPU, memory, service availability
- **Business Alerts**: Payment success rates, order cancellations
- **Security Alerts**: Authentication failures, suspicious activity
- **Performance Alerts**: Response times, database queries

## 🔧 Service-Specific Monitoring

Service-specific monitoring configurations are stored in `k8s/services/`:

### MS-Customer Service Monitoring

- **`ms-customer-datadog-config.yml`**: Datadog JMX configuration for JVM metrics
- **`ms-customer-mongodb-exporter.yml`**: MongoDB metrics exporter deployment
- **`ms-customer-prometheus-alerts.yml`**: Customer service specific alert rules
- **`ms-customer-prometheus-config.yml`**: Prometheus scraping configuration for customer service

### MS-Order Service Monitoring

- **`ms-order-datadog-config.yml`**: Datadog JMX configuration for JVM metrics
- **`ms-order-postgresql-exporter.yml`**: PostgreSQL metrics exporter deployment
- **`ms-order-prometheus-alerts.yml`**: Order service specific alert rules (JVM, HTTP errors, DB connections, Kafka lag, business metrics)
- **`ms-order-prometheus-config.yml`**: Prometheus scraping configuration for order service

### MS-Payment Service Monitoring

- **`ms-payment-datadog-config.yml`**: Datadog JMX configuration for JVM metrics
- **`ms-payment-postgresql-exporter.yml`**: PostgreSQL metrics exporter deployment
- **`ms-payment-prometheus-alerts.yml`**: Payment service specific alert rules (JVM, HTTP errors, DB connections, Kafka lag, payment processing, security)
- **`ms-payment-prometheus-config.yml`**: Prometheus scraping configuration for payment service

### Adding New Service Monitoring

1. Create service-specific monitoring files in `k8s/services/`
2. Update Prometheus configuration to include new service endpoints
3. Add service-specific alert rules
4. Deploy using: `kubectl apply -f k8s/services/`

## 📊 Metrics Collection

### Service Annotations

Ensure your microservices have the required annotations:

```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: "/actuator/prometheus"
    prometheus.io/port: "8080"
```

### Available Metrics

#### JVM Metrics (Auto-collected)

- `jvm_memory_used_bytes`
- `jvm_gc_pause_seconds`
- `jvm_threads_live`
- `jvm_classes_loaded`

#### HTTP Metrics (Auto-collected)

- `http_server_requests_seconds`
- `http_server_requests_active`
- `http_server_requests_total`

#### Business Metrics (Custom)

- `business_orders_created_total`
- `business_payments_processed_total`
- `business_cache_hit_rate`

#### Security Metrics (Custom)

- `security_auth_attempts_total`
- `security_suspicious_activities_total`

## 🚨 Alerting

### Alert Types

| Alert Type            | Severity | Description           | Threshold         |
| --------------------- | -------- | --------------------- | ----------------- |
| ServiceDown           | Critical | Service unavailable   | up == 0 for 2m    |
| HighCPUUsage          | Warning  | CPU usage > 80%       | > 80% for 5m      |
| HighMemoryUsage       | Warning  | Memory usage > 90%    | > 90% for 3m      |
| LowPaymentSuccessRate | Critical | Payment success < 90% | < 90% for 5m      |
| CircuitBreakerOpen    | Critical | Circuit breaker open  | state == 1 for 1m |
| HighAuthFailureRate   | High     | Auth failures > 10%   | > 10% for 3m      |

### Alert Routing

Alerts are routed based on severity:

- **Critical**: DevOps + specific teams
- **High**: DevOps + security team
- **Warning**: DevOps team only

### Managing Alerts

```bash
# View active alerts
kubectl port-forward -n monitoring svc/prometheus 9090:9090
# Visit: http://localhost:9090/alerts

# View AlertManager UI
kubectl port-forward -n monitoring svc/alertmanager 9093:9093
# Visit: http://localhost:9093
```

## 🌐 Accessing the UI

### Local Access (Development)

```bash
# Prometheus
kubectl port-forward -n monitoring svc/prometheus 9090:9090
# Access: http://localhost:9090

# AlertManager
kubectl port-forward -n monitoring svc/alertmanager 9093:9093
# Access: http://localhost:9093
```

### Production Access

For production access, set up Ingress or LoadBalancer services:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: monitoring-ingress
  namespace: monitoring
  annotations:
    kubernetes.io/ingress.class: alb
spec:
  rules:
    - host: prometheus.yourdomain.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: prometheus
                port:
                  number: 9090
    - host: alertmanager.yourdomain.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: alertmanager
                port:
                  number: 9093
```

## 🔧 Troubleshooting

### Common Issues

#### Prometheus Not Starting

```bash
# Check logs
kubectl logs -n monitoring deployment/prometheus

# Check configuration
kubectl exec -n monitoring deployment/prometheus -- cat /etc/prometheus/prometheus.yml
```

#### AlertManager Email Not Working

```bash
# Check AlertManager logs
kubectl logs -n monitoring deployment/alertmanager

# Test email configuration
kubectl exec -n monitoring deployment/alertmanager -- amtool check-config /etc/alertmanager/alertmanager.yml
```

#### Metrics Not Appearing

```bash
# Check service discovery
kubectl exec -n monitoring deployment/prometheus -- curl http://localhost:9090/api/v1/targets

# Verify service annotations
kubectl describe service your-service-name
```

#### Storage Issues

```bash
# Check PVC status
kubectl get pvc -n monitoring

# Check storage class
kubectl get storageclass

# Check PV
kubectl get pv
```

### Performance Tuning

#### Prometheus Performance

- Increase memory limit if needed
- Adjust scrape intervals for less critical metrics
- Use metric relabeling to drop unused metrics

#### AlertManager Performance

- Configure alert grouping to reduce noise
- Set appropriate repeat intervals
- Use inhibition rules for related alerts

## 💾 Backup & Recovery

### Prometheus Data Backup

```bash
# Create backup
kubectl exec -n monitoring deployment/prometheus -- tar czf /tmp/prometheus-backup.tar.gz -C /prometheus .

# Copy backup to local machine
kubectl cp monitoring/$(kubectl get pod -n monitoring -l app=prometheus -o jsonpath='{.items[0].metadata.name}'):/tmp/prometheus-backup.tar.gz ./prometheus-backup.tar.gz
```

### Configuration Backup

```bash
# Backup ConfigMaps
kubectl get configmap -n monitoring -o yaml > monitoring-configmaps-backup.yaml

# Backup Secrets (if any)
kubectl get secret -n monitoring -o yaml > monitoring-secrets-backup.yaml
```

### Recovery

```bash
# Restore ConfigMaps
kubectl apply -f monitoring-configmaps-backup.yaml

# Restore data (if needed)
kubectl cp ./prometheus-backup.tar.gz monitoring/$(kubectl get pod -n monitoring -l app=prometheus -o jsonpath='{.items[0].metadata.name}'):/tmp/
kubectl exec -n monitoring deployment/prometheus -- tar xzf /tmp/prometheus-backup.tar.gz -C /prometheus
```

## 📈 Scaling

### Horizontal Scaling

For high availability, increase replica count:

```yaml
spec:
  replicas: 2 # or more
```

### Vertical Scaling

Adjust resource limits based on load:

```yaml
resources:
  limits:
    memory: 4Gi # Increase as needed
    cpu: 2000m # Increase as needed
```

### Storage Scaling

Increase PVC size when needed:

```yaml
spec:
  resources:
    requests:
      storage: 100Gi # Increase as needed
```

## 🔒 Security

### Network Policies

Apply network policies to restrict traffic:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: monitoring-network-policy
  namespace: monitoring
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: your-services-namespace
      ports:
        - protocol: TCP
          port: 9090 # Prometheus
        - protocol: TCP
          port: 9093 # AlertManager
```

### Authentication

For production, add authentication to Prometheus and AlertManager.

## 📚 Additional Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [AlertManager Documentation](https://prometheus.io/docs/alerting/latest/alertmanager/)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Kubernetes Monitoring Best Practices](https://prometheus.io/docs/practices/kubernetes/)

## 🤝 Contributing

1. Test changes in a development environment
2. Update documentation for any configuration changes
3. Ensure backward compatibility
4. Test alert rules with sample data

## 📄 License

This monitoring stack configuration is part of the microservices project.
