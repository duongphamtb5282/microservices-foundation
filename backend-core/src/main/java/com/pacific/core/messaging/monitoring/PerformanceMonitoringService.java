package com.pacific.core.messaging.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * Service for comprehensive performance monitoring and APM integration. Collects detailed
 * performance metrics, traces operations, and provides insights into system performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class PerformanceMonitoringService {

  private final MeterRegistry meterRegistry;

  // Performance metrics
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong slowRequests = new AtomicLong(0);
  private final AtomicLong failedRequests = new AtomicLong(0);
  private final AtomicLong totalResponseTime = new AtomicLong(0);

  // Memory and CPU metrics
  private final AtomicLong memoryUsage = new AtomicLong(0);
  private final AtomicLong cpuUsage = new AtomicLong(0);

  // Database performance metrics
  private final AtomicLong databaseConnections = new AtomicLong(0);
  private final AtomicLong databaseQueryTime = new AtomicLong(0);
  private final AtomicLong databaseQueryCount = new AtomicLong(0);

  // Cache performance metrics
  private final AtomicLong cacheHits = new AtomicLong(0);
  private final AtomicLong cacheMisses = new AtomicLong(0);

  // Kafka performance metrics
  private final AtomicLong kafkaMessagesSent = new AtomicLong(0);
  private final AtomicLong kafkaMessagesReceived = new AtomicLong(0);
  private final AtomicLong kafkaProcessingTime = new AtomicLong(0);

  // APM-specific metrics
  private final Map<String, Timer.Sample> activeTraces = new ConcurrentHashMap<>();

  /** Initialize performance monitoring. */
  public void initializeMonitoring() {
    // Register performance gauges
    Gauge.builder("performance.requests.total", totalRequests, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("performance.requests.slow", slowRequests, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("performance.requests.failed", failedRequests, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("performance.memory.usage.mb", this::getCurrentMemoryUsage)
        .register(meterRegistry);

    Gauge.builder("performance.cpu.usage.percent", this::getCurrentCpuUsage)
        .register(meterRegistry);

    Gauge.builder("performance.database.connections", databaseConnections, AtomicLong::doubleValue)
        .register(meterRegistry);

    Gauge.builder("performance.cache.hit.rate", this::calculateCacheHitRate)
        .register(meterRegistry);

    log.info("Performance monitoring initialized with APM integration");
  }

  /** Start performance trace for an operation. */
  public String startTrace(String operationName, String serviceName) {
    String traceId = java.util.UUID.randomUUID().toString();

    Timer.Sample sample = Timer.start(meterRegistry);
    activeTraces.put(traceId, sample);

    Counter.builder("performance.traces.started")
        .tag("operation", operationName)
        .tag("service", serviceName)
        .register(meterRegistry)
        .increment();

    log.trace("Started performance trace: {} for operation: {}", traceId, operationName);
    return traceId;
  }

  /** End performance trace and record metrics. */
  public void endTrace(String traceId, String operationName, String serviceName, boolean success) {
    Timer.Sample sample = activeTraces.remove(traceId);
    if (sample != null) {
      long duration =
          sample.stop(
              Timer.builder("performance.operation.duration")
                  .tag("operation", operationName)
                  .tag("service", serviceName)
                  .tag("success", String.valueOf(success))
                  .register(meterRegistry));

      // Record additional metrics
      totalRequests.incrementAndGet();
      totalResponseTime.addAndGet(duration);

      if (!success) {
        failedRequests.incrementAndGet();
      }

      if (duration > 5000) { // More than 5 seconds
        slowRequests.incrementAndGet();

        Counter.builder("performance.requests.slow")
            .tag("operation", operationName)
            .tag("service", serviceName)
            .register(meterRegistry)
            .increment();
      }

      log.trace(
          "Ended performance trace: {} for operation: {} in {}ms",
          traceId,
          operationName,
          duration);
    } else {
      log.warn("Trace not found for ID: {}", traceId);
    }
  }

  /** Record database query performance. */
  public void recordDatabaseQuery(String queryType, long executionTime, boolean success) {
    databaseQueryCount.incrementAndGet();
    databaseQueryTime.addAndGet(executionTime);

    Timer.builder("performance.database.query.time")
        .tag("queryType", queryType)
        .tag("success", String.valueOf(success))
        .register(meterRegistry)
        .record(Duration.ofMillis(executionTime));

    if (!success) {
      Counter.builder("performance.database.query.failed")
          .tag("queryType", queryType)
          .register(meterRegistry)
          .increment();
    }
  }

  /** Record cache operation. */
  public void recordCacheOperation(boolean hit) {
    if (hit) {
      cacheHits.incrementAndGet();
    } else {
      cacheMisses.incrementAndGet();
    }

    Counter.builder("performance.cache.operations")
        .tag("result", hit ? "hit" : "miss")
        .register(meterRegistry)
        .increment();
  }

  /** Record Kafka message processing. */
  public void recordKafkaMessage(
      String topic, String operation, long processingTime, boolean success) {
    if ("send".equals(operation)) {
      kafkaMessagesSent.incrementAndGet();
    } else if ("receive".equals(operation)) {
      kafkaMessagesReceived.incrementAndGet();
    }

    kafkaProcessingTime.addAndGet(processingTime);

    Timer.builder("performance.kafka.message.processing")
        .tag("topic", topic)
        .tag("operation", operation)
        .tag("success", String.valueOf(success))
        .register(meterRegistry)
        .record(Duration.ofMillis(processingTime));

    if (!success) {
      Counter.builder("performance.kafka.message.failed")
          .tag("topic", topic)
          .tag("operation", operation)
          .register(meterRegistry)
          .increment();
    }
  }

  /** Record external service call. */
  public void recordExternalServiceCall(
      String serviceName, String method, long responseTime, boolean success) {
    Timer.builder("performance.external.service.call")
        .tag("service", serviceName)
        .tag("method", method)
        .tag("success", String.valueOf(success))
        .register(meterRegistry)
        .record(Duration.ofMillis(responseTime));

    if (!success) {
      Counter.builder("performance.external.service.failed")
          .tag("service", serviceName)
          .tag("method", method)
          .register(meterRegistry)
          .increment();
    }
  }

  /** Get performance summary. */
  public PerformanceSummary getPerformanceSummary() {
    long totalQueries = databaseQueryCount.get();
    double averageQueryTime =
        totalQueries > 0 ? (double) databaseQueryTime.get() / totalQueries : 0.0;

    long totalMessages = kafkaMessagesSent.get() + kafkaMessagesReceived.get();
    double averageMessageTime =
        totalMessages > 0 ? (double) kafkaProcessingTime.get() / totalMessages : 0.0;

    return PerformanceSummary.builder()
        .totalRequests(totalRequests.get())
        .failedRequests(failedRequests.get())
        .slowRequests(slowRequests.get())
        .averageResponseTime(calculateAverageResponseTime())
        .memoryUsageMB(getCurrentMemoryUsage())
        .cpuUsagePercent(getCurrentCpuUsage())
        .databaseQueryCount(totalQueries)
        .averageDatabaseQueryTime(averageQueryTime)
        .kafkaMessageCount(totalMessages)
        .averageKafkaProcessingTime(averageMessageTime)
        .cacheHitRate(calculateCacheHitRate())
        .activeTraces(activeTraces.size())
        .monitoringEnabled(true)
        .lastUpdate(LocalDateTime.now())
        .build();
  }

  /** Get performance recommendations. */
  public List<String> getPerformanceRecommendations() {
    List<String> recommendations = new java.util.ArrayList<>();

    var summary = getPerformanceSummary();

    // Check response time
    if (summary.getAverageResponseTime() > 2000) {
      recommendations.add(
          "High average response time detected. Consider optimizing database queries or adding caching.");
    }

    // Check error rate
    double errorRate =
        summary.getTotalRequests() > 0
            ? (double) summary.getFailedRequests() / summary.getTotalRequests()
            : 0.0;
    if (errorRate > 0.05) { // More than 5% error rate
      recommendations.add("High error rate detected. Check logs and error patterns.");
    }

    // Check cache performance
    if (summary.getCacheHitRate() < 0.8) {
      recommendations.add(
          "Low cache hit rate. Consider cache optimization or increasing cache size.");
    }

    // Check database performance
    if (summary.getAverageDatabaseQueryTime() > 100) {
      recommendations.add(
          "Slow database queries detected. Consider query optimization or database tuning.");
    }

    // Check memory usage
    if (summary.getMemoryUsageMB() > 512) {
      recommendations.add("High memory usage detected. Consider memory optimization or scaling.");
    }

    return recommendations;
  }

  private double calculateAverageResponseTime() {
    long totalReqs = totalRequests.get();
    return totalReqs > 0 ? (double) totalResponseTime.get() / totalReqs : 0.0;
  }

  private long getCurrentMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024; // MB
  }

  private long getCurrentCpuUsage() {
    // Simplified CPU usage calculation
    // In production, use proper JVM metrics or external monitoring
    return 50; // Placeholder value
  }

  private double calculateCacheHitRate() {
    long totalCacheOps = cacheHits.get() + cacheMisses.get();
    return totalCacheOps > 0 ? (double) cacheHits.get() / totalCacheOps : 0.0;
  }

  /** Performance summary. */
  public static class PerformanceSummary {
    private final long totalRequests;
    private final long failedRequests;
    private final long slowRequests;
    private final double averageResponseTime;
    private final long memoryUsageMB;
    private final long cpuUsagePercent;
    private final long databaseQueryCount;
    private final double averageDatabaseQueryTime;
    private final long kafkaMessageCount;
    private final double averageKafkaProcessingTime;
    private final double cacheHitRate;
    private final int activeTraces;
    private final boolean monitoringEnabled;
    private final LocalDateTime lastUpdate;

    public PerformanceSummary(
        long totalRequests,
        long failedRequests,
        long slowRequests,
        double averageResponseTime,
        long memoryUsageMB,
        long cpuUsagePercent,
        long databaseQueryCount,
        double averageDatabaseQueryTime,
        long kafkaMessageCount,
        double averageKafkaProcessingTime,
        double cacheHitRate,
        int activeTraces,
        boolean monitoringEnabled,
        LocalDateTime lastUpdate) {
      this.totalRequests = totalRequests;
      this.failedRequests = failedRequests;
      this.slowRequests = slowRequests;
      this.averageResponseTime = averageResponseTime;
      this.memoryUsageMB = memoryUsageMB;
      this.cpuUsagePercent = cpuUsagePercent;
      this.databaseQueryCount = databaseQueryCount;
      this.averageDatabaseQueryTime = averageDatabaseQueryTime;
      this.kafkaMessageCount = kafkaMessageCount;
      this.averageKafkaProcessingTime = averageKafkaProcessingTime;
      this.cacheHitRate = cacheHitRate;
      this.activeTraces = activeTraces;
      this.monitoringEnabled = monitoringEnabled;
      this.lastUpdate = lastUpdate;
    }

    public static PerformanceSummaryBuilder builder() {
      return new PerformanceSummaryBuilder();
    }

    // Getters
    public long getTotalRequests() {
      return totalRequests;
    }

    public long getFailedRequests() {
      return failedRequests;
    }

    public long getSlowRequests() {
      return slowRequests;
    }

    public double getAverageResponseTime() {
      return averageResponseTime;
    }

    public long getMemoryUsageMB() {
      return memoryUsageMB;
    }

    public long getCpuUsagePercent() {
      return cpuUsagePercent;
    }

    public long getDatabaseQueryCount() {
      return databaseQueryCount;
    }

    public double getAverageDatabaseQueryTime() {
      return averageDatabaseQueryTime;
    }

    public long getKafkaMessageCount() {
      return kafkaMessageCount;
    }

    public double getAverageKafkaProcessingTime() {
      return averageKafkaProcessingTime;
    }

    public double getCacheHitRate() {
      return cacheHitRate;
    }

    public int getActiveTraces() {
      return activeTraces;
    }

    public boolean isMonitoringEnabled() {
      return monitoringEnabled;
    }

    public LocalDateTime getLastUpdate() {
      return lastUpdate;
    }

    public static class PerformanceSummaryBuilder {
      private long totalRequests;
      private long failedRequests;
      private long slowRequests;
      private double averageResponseTime;
      private long memoryUsageMB;
      private long cpuUsagePercent;
      private long databaseQueryCount;
      private double averageDatabaseQueryTime;
      private long kafkaMessageCount;
      private double averageKafkaProcessingTime;
      private double cacheHitRate;
      private int activeTraces;
      private boolean monitoringEnabled;
      private LocalDateTime lastUpdate;

      public PerformanceSummaryBuilder totalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
        return this;
      }

      public PerformanceSummaryBuilder failedRequests(long failedRequests) {
        this.failedRequests = failedRequests;
        return this;
      }

      public PerformanceSummaryBuilder slowRequests(long slowRequests) {
        this.slowRequests = slowRequests;
        return this;
      }

      public PerformanceSummaryBuilder averageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
        return this;
      }

      public PerformanceSummaryBuilder memoryUsageMB(long memoryUsageMB) {
        this.memoryUsageMB = memoryUsageMB;
        return this;
      }

      public PerformanceSummaryBuilder cpuUsagePercent(long cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
        return this;
      }

      public PerformanceSummaryBuilder databaseQueryCount(long databaseQueryCount) {
        this.databaseQueryCount = databaseQueryCount;
        return this;
      }

      public PerformanceSummaryBuilder averageDatabaseQueryTime(double averageDatabaseQueryTime) {
        this.averageDatabaseQueryTime = averageDatabaseQueryTime;
        return this;
      }

      public PerformanceSummaryBuilder kafkaMessageCount(long kafkaMessageCount) {
        this.kafkaMessageCount = kafkaMessageCount;
        return this;
      }

      public PerformanceSummaryBuilder averageKafkaProcessingTime(
          double averageKafkaProcessingTime) {
        this.averageKafkaProcessingTime = averageKafkaProcessingTime;
        return this;
      }

      public PerformanceSummaryBuilder cacheHitRate(double cacheHitRate) {
        this.cacheHitRate = cacheHitRate;
        return this;
      }

      public PerformanceSummaryBuilder activeTraces(int activeTraces) {
        this.activeTraces = activeTraces;
        return this;
      }

      public PerformanceSummaryBuilder monitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
        return this;
      }

      public PerformanceSummaryBuilder lastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
      }

      public PerformanceSummary build() {
        return new PerformanceSummary(
            totalRequests,
            failedRequests,
            slowRequests,
            averageResponseTime,
            memoryUsageMB,
            cpuUsagePercent,
            databaseQueryCount,
            averageDatabaseQueryTime,
            kafkaMessageCount,
            averageKafkaProcessingTime,
            cacheHitRate,
            activeTraces,
            monitoringEnabled,
            lastUpdate);
      }
    }
  }
}
