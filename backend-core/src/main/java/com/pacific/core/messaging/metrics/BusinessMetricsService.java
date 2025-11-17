package com.pacific.core.messaging.metrics;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service for collecting business-specific metrics. Provides insights into business operations
 * beyond technical metrics.
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class BusinessMetricsService {

  private final MeterRegistry meterRegistry;

  // Order-related metrics
  private final AtomicLong totalOrdersCreated = new AtomicLong(0);
  private final AtomicLong totalOrdersCancelled = new AtomicLong(0);
  private final AtomicLong totalOrderValue = new AtomicLong(0);

  // Payment-related metrics
  private final AtomicLong totalPaymentsProcessed = new AtomicLong(0);
  private final AtomicLong totalPaymentValue = new AtomicLong(0);
  private final AtomicLong successfulPayments = new AtomicLong(0);
  private final AtomicLong failedPayments = new AtomicLong(0);

  // Event processing metrics
  private final AtomicLong eventsProcessed = new AtomicLong(0);
  private final AtomicLong eventsFailed = new AtomicLong(0);

  // User activity metrics
  private final AtomicLong activeUsers = new AtomicLong(0);

  // Initialize gauges
  public void initializeGauges() {
    // Order metrics
    Gauge.builder("business.orders.total.created", totalOrdersCreated, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("business.orders.total.cancelled", totalOrdersCancelled, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("business.orders.total.value", totalOrderValue, AtomicLong::doubleValue)
        .register(meterRegistry);

    // Payment metrics
    Gauge.builder(
            "business.payments.total.processed", totalPaymentsProcessed, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("business.payments.total.value", totalPaymentValue, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("business.payments.successful", successfulPayments, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("business.payments.failed", failedPayments, AtomicLong::doubleValue)
        .register(meterRegistry);

    // Event metrics
    Gauge.builder("business.events.processed", eventsProcessed, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("business.events.failed", eventsFailed, AtomicLong::doubleValue)
        .register(meterRegistry);

    // User metrics
    Gauge.builder("business.users.active", activeUsers, AtomicLong::doubleValue)
        .register(meterRegistry);

    log.info("Business metrics gauges initialized");
  }

  /** Record order creation. */
  public void recordOrderCreated(String userId, double orderValue) {
    totalOrdersCreated.incrementAndGet();
    totalOrderValue.addAndGet((long) (orderValue * 100)); // Store as cents

    // Record per-user metrics
    Counter.builder("business.orders.created")
        .tag("userId", userId)
        .register(meterRegistry)
        .increment();

    // Record order value distribution
    Timer.builder("business.order.value")
        .register(meterRegistry)
        .record(Duration.ofMillis((long) orderValue));

    log.debug("Recorded order creation: user={}, value={}", userId, orderValue);
  }

  /** Record order cancellation. */
  public void recordOrderCancelled(String userId, String reason) {
    totalOrdersCancelled.incrementAndGet();

    Counter.builder("business.orders.cancelled")
        .tag("userId", userId)
        .tag("reason", reason != null ? reason : "unknown")
        .register(meterRegistry)
        .increment();

    log.debug("Recorded order cancellation: user={}, reason={}", userId, reason);
  }

  /** Record payment processing. */
  public void recordPaymentProcessed(String userId, double paymentValue, boolean successful) {
    totalPaymentsProcessed.incrementAndGet();
    totalPaymentValue.addAndGet((long) (paymentValue * 100));

    if (successful) {
      successfulPayments.incrementAndGet();
    } else {
      failedPayments.incrementAndGet();
    }

    Counter.builder("business.payments.processed")
        .tag("userId", userId)
        .tag("successful", String.valueOf(successful))
        .register(meterRegistry)
        .increment();

    log.debug(
        "Recorded payment processing: user={}, value={}, success={}",
        userId,
        paymentValue,
        successful);
  }

  /** Record event processing. */
  public void recordEventProcessed(String eventType, boolean successful) {
    eventsProcessed.incrementAndGet();

    if (!successful) {
      eventsFailed.incrementAndGet();
    }

    Counter.builder("business.events.processed")
        .tag("eventType", eventType)
        .tag("successful", String.valueOf(successful))
        .register(meterRegistry)
        .increment();
  }

  /** Record user activity. */
  public void recordUserActivity(String userId) {
    activeUsers.set(Math.max(activeUsers.get(), getActiveUserCount() + 1));

    Counter.builder("business.user.activity")
        .tag("userId", userId)
        .register(meterRegistry)
        .increment();
  }

  /** Get business metrics summary. */
  public BusinessMetricsSummary getSummary() {
    return BusinessMetricsSummary.builder()
        .totalOrdersCreated(totalOrdersCreated.get())
        .totalOrdersCancelled(totalOrdersCancelled.get())
        .totalOrderValue(totalOrderValue.get() / 100.0) // Convert from cents
        .totalPaymentsProcessed(totalPaymentsProcessed.get())
        .totalPaymentValue(totalPaymentValue.get() / 100.0)
        .successfulPayments(successfulPayments.get())
        .failedPayments(failedPayments.get())
        .eventsProcessed(eventsProcessed.get())
        .eventsFailed(eventsFailed.get())
        .activeUsers(activeUsers.get())
        .orderSuccessRate(calculateOrderSuccessRate())
        .paymentSuccessRate(calculatePaymentSuccessRate())
        .averageOrderValue(calculateAverageOrderValue())
        .averagePaymentValue(calculateAveragePaymentValue())
        .build();
  }

  private double calculateOrderSuccessRate() {
    long total = totalOrdersCreated.get() + totalOrdersCancelled.get();
    return total > 0 ? (double) totalOrdersCreated.get() / total : 0.0;
  }

  private double calculatePaymentSuccessRate() {
    long total = successfulPayments.get() + failedPayments.get();
    return total > 0 ? (double) successfulPayments.get() / total : 0.0;
  }

  private double calculateAverageOrderValue() {
    long totalOrders = totalOrdersCreated.get();
    return totalOrders > 0 ? (double) totalOrderValue.get() / (100.0 * totalOrders) : 0.0;
  }

  private double calculateAveragePaymentValue() {
    long totalPayments = totalPaymentsProcessed.get();
    return totalPayments > 0 ? (double) totalPaymentValue.get() / (100.0 * totalPayments) : 0.0;
  }

  private long getActiveUserCount() {
    // In a real implementation, this would query the database or cache
    // For now, return the current gauge value
    return activeUsers.get();
  }

  /** Business metrics summary. */
  public static class BusinessMetricsSummary {
    private final long totalOrdersCreated;
    private final long totalOrdersCancelled;
    private final double totalOrderValue;
    private final long totalPaymentsProcessed;
    private final double totalPaymentValue;
    private final long successfulPayments;
    private final long failedPayments;
    private final long eventsProcessed;
    private final long eventsFailed;
    private final long activeUsers;
    private final double orderSuccessRate;
    private final double paymentSuccessRate;
    private final double averageOrderValue;
    private final double averagePaymentValue;

    public BusinessMetricsSummary(
        long totalOrdersCreated,
        long totalOrdersCancelled,
        double totalOrderValue,
        long totalPaymentsProcessed,
        double totalPaymentValue,
        long successfulPayments,
        long failedPayments,
        long eventsProcessed,
        long eventsFailed,
        long activeUsers,
        double orderSuccessRate,
        double paymentSuccessRate,
        double averageOrderValue,
        double averagePaymentValue) {
      this.totalOrdersCreated = totalOrdersCreated;
      this.totalOrdersCancelled = totalOrdersCancelled;
      this.totalOrderValue = totalOrderValue;
      this.totalPaymentsProcessed = totalPaymentsProcessed;
      this.totalPaymentValue = totalPaymentValue;
      this.successfulPayments = successfulPayments;
      this.failedPayments = failedPayments;
      this.eventsProcessed = eventsProcessed;
      this.eventsFailed = eventsFailed;
      this.activeUsers = activeUsers;
      this.orderSuccessRate = orderSuccessRate;
      this.paymentSuccessRate = paymentSuccessRate;
      this.averageOrderValue = averageOrderValue;
      this.averagePaymentValue = averagePaymentValue;
    }

    public static BusinessMetricsSummaryBuilder builder() {
      return new BusinessMetricsSummaryBuilder();
    }

    // Getters
    public long getTotalOrdersCreated() {
      return totalOrdersCreated;
    }

    public long getTotalOrdersCancelled() {
      return totalOrdersCancelled;
    }

    public double getTotalOrderValue() {
      return totalOrderValue;
    }

    public long getTotalPaymentsProcessed() {
      return totalPaymentsProcessed;
    }

    public double getTotalPaymentValue() {
      return totalPaymentValue;
    }

    public long getSuccessfulPayments() {
      return successfulPayments;
    }

    public long getFailedPayments() {
      return failedPayments;
    }

    public long getEventsProcessed() {
      return eventsProcessed;
    }

    public long getEventsFailed() {
      return eventsFailed;
    }

    public long getActiveUsers() {
      return activeUsers;
    }

    public double getOrderSuccessRate() {
      return orderSuccessRate;
    }

    public double getPaymentSuccessRate() {
      return paymentSuccessRate;
    }

    public double getAverageOrderValue() {
      return averageOrderValue;
    }

    public double getAveragePaymentValue() {
      return averagePaymentValue;
    }

    public static class BusinessMetricsSummaryBuilder {
      private long totalOrdersCreated;
      private long totalOrdersCancelled;
      private double totalOrderValue;
      private long totalPaymentsProcessed;
      private double totalPaymentValue;
      private long successfulPayments;
      private long failedPayments;
      private long eventsProcessed;
      private long eventsFailed;
      private long activeUsers;
      private double orderSuccessRate;
      private double paymentSuccessRate;
      private double averageOrderValue;
      private double averagePaymentValue;

      public BusinessMetricsSummaryBuilder totalOrdersCreated(long totalOrdersCreated) {
        this.totalOrdersCreated = totalOrdersCreated;
        return this;
      }

      public BusinessMetricsSummaryBuilder totalOrdersCancelled(long totalOrdersCancelled) {
        this.totalOrdersCancelled = totalOrdersCancelled;
        return this;
      }

      public BusinessMetricsSummaryBuilder totalOrderValue(double totalOrderValue) {
        this.totalOrderValue = totalOrderValue;
        return this;
      }

      public BusinessMetricsSummaryBuilder totalPaymentsProcessed(long totalPaymentsProcessed) {
        this.totalPaymentsProcessed = totalPaymentsProcessed;
        return this;
      }

      public BusinessMetricsSummaryBuilder totalPaymentValue(double totalPaymentValue) {
        this.totalPaymentValue = totalPaymentValue;
        return this;
      }

      public BusinessMetricsSummaryBuilder successfulPayments(long successfulPayments) {
        this.successfulPayments = successfulPayments;
        return this;
      }

      public BusinessMetricsSummaryBuilder failedPayments(long failedPayments) {
        this.failedPayments = failedPayments;
        return this;
      }

      public BusinessMetricsSummaryBuilder eventsProcessed(long eventsProcessed) {
        this.eventsProcessed = eventsProcessed;
        return this;
      }

      public BusinessMetricsSummaryBuilder eventsFailed(long eventsFailed) {
        this.eventsFailed = eventsFailed;
        return this;
      }

      public BusinessMetricsSummaryBuilder activeUsers(long activeUsers) {
        this.activeUsers = activeUsers;
        return this;
      }

      public BusinessMetricsSummaryBuilder orderSuccessRate(double orderSuccessRate) {
        this.orderSuccessRate = orderSuccessRate;
        return this;
      }

      public BusinessMetricsSummaryBuilder paymentSuccessRate(double paymentSuccessRate) {
        this.paymentSuccessRate = paymentSuccessRate;
        return this;
      }

      public BusinessMetricsSummaryBuilder averageOrderValue(double averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
        return this;
      }

      public BusinessMetricsSummaryBuilder averagePaymentValue(double averagePaymentValue) {
        this.averagePaymentValue = averagePaymentValue;
        return this;
      }

      public BusinessMetricsSummary build() {
        return new BusinessMetricsSummary(
            totalOrdersCreated,
            totalOrdersCancelled,
            totalOrderValue,
            totalPaymentsProcessed,
            totalPaymentValue,
            successfulPayments,
            failedPayments,
            eventsProcessed,
            eventsFailed,
            activeUsers,
            orderSuccessRate,
            paymentSuccessRate,
            averageOrderValue,
            averagePaymentValue);
      }
    }
  }
}
