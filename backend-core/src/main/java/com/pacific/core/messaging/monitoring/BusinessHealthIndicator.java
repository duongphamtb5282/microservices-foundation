package com.pacific.core.messaging.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.circuitbreaker.CircuitBreakerService;
import com.pacific.core.messaging.metrics.BusinessMetricsService;

/** Health indicator that includes business metrics and circuit breaker status. */
@Component
@ConditionalOnProperty(
    name = "business.health.enabled",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
public class BusinessHealthIndicator implements HealthIndicator {

  private final BusinessMetricsService businessMetricsService;
  private final CircuitBreakerService circuitBreakerService;

  @Value("${backend-core.monitoring.health.primary-service:}")
  private String primaryService;

  @Override
  public Health health() {
    try {
      var businessMetrics = businessMetricsService.getSummary();

      Health.Builder healthBuilder = Health.up();

      // Add business metrics
      healthBuilder =
          healthBuilder
              .withDetail("business.orders.totalCreated", businessMetrics.getTotalOrdersCreated())
              .withDetail(
                  "business.orders.totalCancelled", businessMetrics.getTotalOrdersCancelled())
              .withDetail(
                  "business.orders.successRate",
                  String.format("%.2f%%", businessMetrics.getOrderSuccessRate() * 100))
              .withDetail(
                  "business.orders.averageValue",
                  String.format("$%.2f", businessMetrics.getAverageOrderValue()))
              .withDetail(
                  "business.payments.totalProcessed", businessMetrics.getTotalPaymentsProcessed())
              .withDetail(
                  "business.payments.successRate",
                  String.format("%.2f%%", businessMetrics.getPaymentSuccessRate() * 100))
              .withDetail(
                  "business.payments.averageValue",
                  String.format("$%.2f", businessMetrics.getAveragePaymentValue()))
              .withDetail("business.events.processed", businessMetrics.getEventsProcessed())
              .withDetail("business.events.failed", businessMetrics.getEventsFailed())
              .withDetail("business.users.active", businessMetrics.getActiveUsers());

      // Check circuit breaker status for primary service and determine health
      boolean isHealthy;
      if (primaryService != null && !primaryService.isEmpty()) {
        var primaryCircuitBreaker = circuitBreakerService.getMetrics(primaryService);
        healthBuilder =
            healthBuilder
                .withDetail(
                    "circuitbreaker." + primaryService + ".state",
                    primaryCircuitBreaker.getState().toString())
                .withDetail(
                    "circuitbreaker." + primaryService + ".failureRate",
                    String.format("%.2f%%", primaryCircuitBreaker.getFailureRate() * 100));

        // Determine overall health based on critical metrics including circuit breaker
        isHealthy = isBusinessHealthy(businessMetrics, primaryCircuitBreaker);
      } else {
        // Determine overall health based on critical metrics without circuit breaker
        isHealthy = isBusinessHealthy(businessMetrics);
      }

      if (!isHealthy) {
        healthBuilder =
            healthBuilder
                .down()
                .withDetail("reason", "Business metrics indicate degraded performance");
      }

      return healthBuilder.build();

    } catch (Exception e) {
      return Health.down(e).build();
    }
  }

  /** Determine if business is healthy based on metrics. */
  private boolean isBusinessHealthy(BusinessMetricsService.BusinessMetricsSummary metrics) {
    return isBusinessHealthy(metrics, null);
  }

  /** Determine if business is healthy based on metrics. */
  private boolean isBusinessHealthy(
      BusinessMetricsService.BusinessMetricsSummary metrics,
      CircuitBreakerService.CircuitBreakerMetrics circuitBreakerMetrics) {

    // Check payment success rate (should be > 90%)
    if (metrics.getPaymentSuccessRate() < 0.9) {
      return false;
    }

    // Check circuit breaker if available
    if (circuitBreakerMetrics != null) {
      // Check if circuit breaker is open
      if (circuitBreakerMetrics.getState().toString().equals("OPEN")) {
        return false;
      }

      // Check if failure rate is too high
      if (circuitBreakerMetrics.getFailureRate() > 0.5) {
        return false;
      }
    }

    // Check if event processing failure rate is too high
    long totalEvents = metrics.getEventsProcessed() + metrics.getEventsFailed();
    if (totalEvents > 100) { // Only consider if we have enough data
      double eventFailureRate = (double) metrics.getEventsFailed() / totalEvents;
      if (eventFailureRate > 0.1) { // More than 10% event failures
        return false;
      }
    }

    return true;
  }
}
