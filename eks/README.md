# EKS Deployment YAML Files

This directory contains Kubernetes YAML files for deploying the microservices to Amazon EKS (Elastic Kubernetes Service).

## Directory Structure

```
eks/
├── namespaces/          # Namespace definitions with resource quotas and network policies
├── deployments/         # Service deployments with health checks and resource limits
├── services/           # Service definitions for internal and external communication
├── configmaps/         # Application configuration files
├── serviceaccounts/    # Service accounts with IAM role annotations
└── secrets/           # Secret templates for sensitive data
```

## Services Included

- **api-gateway**: API Gateway service (LoadBalancer service for external access)
- **auth-service**: Authentication service
- **ms-customer**: Customer service
- **ms-order-service**: Order service
- **ms-payment-service**: Payment service

_Note: backend-core service is excluded as per requirements_

## Prerequisites

1. **EKS Cluster**: Ensure you have an EKS cluster running
2. **AWS Load Balancer Controller**: Install the AWS Load Balancer Controller for ALB/NLB support
3. **External DNS**: Install External DNS for automatic DNS management
4. **IAM Roles for Service Accounts (IRSA)**: Configure IRSA for secure AWS API access

## Deployment Order

Deploy resources in the following order:

1. **Namespaces** - Create isolated environments
2. **ServiceAccounts** - Set up IAM roles
3. **Secrets** - Configure sensitive data
4. **ConfigMaps** - Deploy configuration
5. **Services** - Create service endpoints
6. **Deployments** - Deploy applications

## Configuration Required

### 1. Update Account ID and Region

Replace `ACCOUNT-ID` and `region` in all YAML files with your actual AWS account ID and region.

### 2. Update Image References

Replace `your-registry` with your actual container registry URL in deployment files.

### 3. Configure Secrets

Update all secrets marked with `CHANGE_ME` in the `secrets/` directory:

- Database credentials
- Encryption keys (32 characters for AES-256)
- Payment gateway credentials
- Keycloak client secrets

### 4. Database Setup

Ensure the following databases are available:

- PostgreSQL for auth-service
- MongoDB for ms-customer
- PostgreSQL for ms-order-service
- PostgreSQL for ms-payment-service

### 5. Supporting Infrastructure

Ensure the following infrastructure is in place:

- Kafka cluster
- Redis cluster
- Keycloak (for auth-service)
- Monitoring stack (Prometheus, Grafana)

## Deployment Commands

```bash
# Create all namespaces
kubectl apply -f namespaces/

# Create service accounts and IAM roles
kubectl apply -f serviceaccounts/

# Create secrets (after updating with real values)
kubectl apply -f secrets/

# Create configmaps
kubectl apply -f configmaps/

# Create services
kubectl apply -f services/

# Create deployments
kubectl apply -f deployments/
```

## Service URLs

After deployment, services will be accessible at:

- **API Gateway**: `http://api-gateway.{domain}` (external LoadBalancer)
- **Auth Service**: `http://auth-service.auth-service-prod.svc.cluster.local:8080`
- **Customer Service**: `http://ms-customer.ms-customer-prod.svc.cluster.local:8084`
- **Order Service**: `http://ms-order-service.ms-order-service-prod.svc.cluster.local:8081`
- **Payment Service**: `http://ms-payment-service.ms-payment-service-prod.svc.cluster.local:8080`

## Monitoring

All services are configured with:

- Prometheus metrics scraping
- Health checks (liveness, readiness, startup)
- Structured logging
- Resource limits and requests

## Security Features

- **Non-root containers**: All containers run as non-root users
- **Read-only root filesystem**: Containers have read-only root filesystems
- **Security contexts**: Proper security contexts with dropped capabilities
- **Network policies**: Namespace isolation with allow rules
- **IRSA**: IAM roles for secure AWS API access
- **Secrets management**: Sensitive data stored in Kubernetes secrets

## Troubleshooting

1. **Pod Security Standards**: Ensure pods comply with EKS pod security standards
2. **Resource Quotas**: Monitor namespace resource usage
3. **Network Policies**: Verify network connectivity between services
4. **IAM Permissions**: Ensure IRSA roles have necessary permissions
5. **Secrets**: Verify secret values are properly base64 encoded

## Maintenance

- Regularly rotate encryption keys and database credentials
- Monitor resource usage and adjust limits as needed
- Update images and configurations through CI/CD pipelines
- Backup and restore procedures for persistent data
