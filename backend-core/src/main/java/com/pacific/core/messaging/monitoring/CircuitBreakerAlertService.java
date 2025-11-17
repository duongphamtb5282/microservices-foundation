package com.pacific.core.messaging.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pacific.core.messaging.circuitbreaker.CircuitBreakerService;
import com.pacific.core.messaging.security.SecurityEvent;
import com.pacific.core.messaging.security.SecurityEventRepository;

/**
 * Service for monitoring circuit breakers and sending alerts on state changes. Provides proactive
 * monitoring and alerting for resilience patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "monitoring.circuit-breaker-alerts.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class CircuitBreakerAlertService {

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final CircuitBreakerService circuitBreakerService;
  private final SecurityEventRepository securityEventRepository;

  @Value("${backend-core.monitoring.circuit-breaker.services:}")
  private List<String> monitoredServices;

  // State tracking
  private final Map<String, CircuitBreaker.State> previousStates = new ConcurrentHashMap<>();
  private final Map<String, LocalDateTime> stateChangeTimes = new ConcurrentHashMap<>();

  // Alert metrics
  private final AtomicLong alertsSent = new AtomicLong(0);
  private final AtomicLong circuitOpenEvents = new AtomicLong(0);
  private final AtomicLong circuitClosedEvents = new AtomicLong(0);

  /** Monitor circuit breaker state changes and send alerts. */
  @Scheduled(fixedDelay = 30000) // Every 30 seconds
  public void monitorCircuitBreakers() {
    log.debug("Monitoring circuit breaker states");

    List<String> circuitBreakerNames = monitoredServices.isEmpty() ? List.of() : monitoredServices;

    for (String serviceName : circuitBreakerNames) {
      try {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        CircuitBreaker.State currentState = circuitBreaker.getState();
        CircuitBreaker.State previousState = previousStates.get(serviceName);

        // Check for state change
        if (previousState != null && !previousState.equals(currentState)) {
          handleStateChange(serviceName, previousState, currentState);
        }

        // Check for problematic states
        checkForProblematicState(serviceName, currentState);

        // Update state tracking
        previousStates.put(serviceName, currentState);

      } catch (Exception e) {
        log.error("Error monitoring circuit breaker: {}", serviceName, e);
      }
    }
  }

  /** Handle circuit breaker state change. */
  private void handleStateChange(
      String serviceName, CircuitBreaker.State fromState, CircuitBreaker.State toState) {
    LocalDateTime changeTime = LocalDateTime.now();
    stateChangeTimes.put(serviceName, changeTime);

    String message =
        String.format(
            "Circuit breaker state changed: %s -> %s for service: %s",
            fromState, toState, serviceName);

    SecurityEvent.Severity severity = determineSeverity(toState);

    // Create security event
    SecurityEvent event =
        SecurityEvent.builder()
            .eventId(java.util.UUID.randomUUID().toString())
            .eventType("CIRCUIT_BREAKER_STATE_CHANGE")
            .description(message)
            .severity(severity)
            .source("CircuitBreakerAlertService")
            .timestamp(changeTime)
            .details(
                Map.of(
                    "serviceName", serviceName,
                    "fromState", fromState.toString(),
                    "toState", toState.toString(),
                    "changeTime", changeTime.toString()))
            .build();

    securityEventRepository.saveSecurityEvent(event);
    alertsSent.incrementAndGet();

    if (toState == CircuitBreaker.State.OPEN) {
      circuitOpenEvents.incrementAndGet();
    } else if (toState == CircuitBreaker.State.CLOSED) {
      circuitClosedEvents.incrementAndGet();
    }

    log.warn(
        "Circuit breaker state change detected: {} -> {} for service: {}",
        fromState,
        toState,
        serviceName);

    // Send alert based on severity
    sendAlert(serviceName, toState, severity, message);
  }

  /** Check for problematic circuit breaker states. */
  private void checkForProblematicState(String serviceName, CircuitBreaker.State state) {
    if (state == CircuitBreaker.State.OPEN) {
      LocalDateTime openTime = stateChangeTimes.get(serviceName);
      if (openTime != null) {
        Duration openDuration = Duration.between(openTime, LocalDateTime.now());

        // Alert if circuit has been open for more than 5 minutes
        if (openDuration.toMinutes() > 5) {
          String message =
              String.format(
                  "Circuit breaker has been OPEN for %d minutes for service: %s",
                  openDuration.toMinutes(), serviceName);

          sendAlert(serviceName, state, SecurityEvent.Severity.HIGH, message);
        }
      }
    }
  }

  /** Send alert for circuit breaker issues. */
  private void sendAlert(
      String serviceName,
      CircuitBreaker.State state,
      SecurityEvent.Severity severity,
      String message) {
    try {
      // In production, this would integrate with alerting systems like:
      // - PagerDuty
      // - Slack
      // - Email notifications
      // - SMS alerts

      switch (severity) {
        case CRITICAL -> log.error("🚨 CRITICAL ALERT: {}", message);
        case HIGH -> log.warn("⚠️ HIGH ALERT: {}", message);
        case MEDIUM -> log.info("ℹ️ MEDIUM ALERT: {}", message);
        case LOW -> log.debug("🔍 LOW ALERT: {}", message);
      }

      // Additional alerting logic could be implemented here
      if (severity == SecurityEvent.Severity.CRITICAL || severity == SecurityEvent.Severity.HIGH) {
        // Send to external alerting system
        sendExternalAlert(serviceName, state, severity, message);
      }

    } catch (Exception e) {
      log.error("Failed to send alert for service: {}", serviceName, e);
    }
  }

  /** Send alert to external systems. */
  private void sendExternalAlert(
      String serviceName,
      CircuitBreaker.State state,
      SecurityEvent.Severity severity,
      String message) {
    // This would integrate with external alerting systems
    log.info("Sending external alert for service: {} - {}", serviceName, message);

    // Example integrations:
    // - Webhook to PagerDuty
    // - Slack notification
    // - Email alert
    // - SMS notification
    // - Integration with monitoring dashboards
  }

  /** Determine severity based on circuit breaker state. */
  private SecurityEvent.Severity determineSeverity(CircuitBreaker.State state) {
    return switch (state) {
      case OPEN -> SecurityEvent.Severity.CRITICAL;
      case HALF_OPEN -> SecurityEvent.Severity.HIGH;
      case CLOSED -> SecurityEvent.Severity.LOW;
      default -> SecurityEvent.Severity.MEDIUM;
    };
  }

  /** Get circuit breaker monitoring summary. */
  public CircuitBreakerMonitoringSummary getMonitoringSummary() {
    Map<String, CircuitBreakerMonitoringInfo> circuitBreakers = new ConcurrentHashMap<>();

    for (String serviceName : monitoredServices) {
      try {
        var metrics = circuitBreakerService.getMetrics(serviceName);
        CircuitBreaker.State currentState = metrics.getState();

        CircuitBreakerMonitoringInfo info =
            CircuitBreakerMonitoringInfo.builder()
                .serviceName(serviceName)
                .currentState(currentState.toString())
                .failureRate(metrics.getFailureRate())
                .successfulCalls(metrics.getNumberOfSuccessfulCalls())
                .failedCalls(metrics.getNumberOfFailedCalls())
                .lastStateChange(stateChangeTimes.get(serviceName))
                .stateChangeDuration(calculateStateDuration(serviceName))
                .build();

        circuitBreakers.put(serviceName, info);

      } catch (Exception e) {
        log.error("Error getting metrics for circuit breaker: {}", serviceName, e);

        CircuitBreakerMonitoringInfo errorInfo =
            CircuitBreakerMonitoringInfo.builder()
                .serviceName(serviceName)
                .currentState("ERROR")
                .error(e.getMessage())
                .build();

        circuitBreakers.put(serviceName, errorInfo);
      }
    }

    return CircuitBreakerMonitoringSummary.builder()
        .circuitBreakers(circuitBreakers)
        .totalAlertsSent(alertsSent.get())
        .circuitOpenEvents(circuitOpenEvents.get())
        .circuitClosedEvents(circuitClosedEvents.get())
        .monitoringEnabled(true)
        .lastUpdate(LocalDateTime.now())
        .build();
  }

  private Duration calculateStateDuration(String serviceName) {
    LocalDateTime changeTime = stateChangeTimes.get(serviceName);
    if (changeTime != null) {
      return Duration.between(changeTime, LocalDateTime.now());
    }
    return Duration.ZERO;
  }

  /** Monitoring information for a circuit breaker. */
  public static class CircuitBreakerMonitoringInfo {
    private String serviceName;
    private String currentState;
    private float failureRate;
    private int successfulCalls;
    private int failedCalls;
    private LocalDateTime lastStateChange;
    private Duration stateChangeDuration;
    private String error;

    public static CircuitBreakerMonitoringInfoBuilder builder() {
      return new CircuitBreakerMonitoringInfoBuilder();
    }

    // Getters and setters
    public String getServiceName() {
      return serviceName;
    }

    public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
    }

    public String getCurrentState() {
      return currentState;
    }

    public void setCurrentState(String currentState) {
      this.currentState = currentState;
    }

    public float getFailureRate() {
      return failureRate;
    }

    public void setFailureRate(float failureRate) {
      this.failureRate = failureRate;
    }

    public int getSuccessfulCalls() {
      return successfulCalls;
    }

    public void setSuccessfulCalls(int successfulCalls) {
      this.successfulCalls = successfulCalls;
    }

    public int getFailedCalls() {
      return failedCalls;
    }

    public void setFailedCalls(int failedCalls) {
      this.failedCalls = failedCalls;
    }

    public LocalDateTime getLastStateChange() {
      return lastStateChange;
    }

    public void setLastStateChange(LocalDateTime lastStateChange) {
      this.lastStateChange = lastStateChange;
    }

    public Duration getStateChangeDuration() {
      return stateChangeDuration;
    }

    public void setStateChangeDuration(Duration stateChangeDuration) {
      this.stateChangeDuration = stateChangeDuration;
    }

    public String getError() {
      return error;
    }

    public void setError(String error) {
      this.error = error;
    }

    public static class CircuitBreakerMonitoringInfoBuilder {
      private String serviceName;
      private String currentState;
      private float failureRate;
      private int successfulCalls;
      private int failedCalls;
      private LocalDateTime lastStateChange;
      private Duration stateChangeDuration;
      private String error;

      public CircuitBreakerMonitoringInfoBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
      }

      public CircuitBreakerMonitoringInfoBuilder currentState(String currentState) {
        this.currentState = currentState;
        return this;
      }

      public CircuitBreakerMonitoringInfoBuilder failureRate(float failureRate) {
        this.failureRate = failureRate;
        return this;
      }

      public CircuitBreakerMonitoringInfoBuilder successfulCalls(int successfulCalls) {
        this.successfulCalls = successfulCalls;
        return this;
      }

      public CircuitBreakerMonitoringInfoBuilder failedCalls(int failedCalls) {
        this.failedCalls = failedCalls;
        return this;
      }

      public CircuitBreakerMonitoringInfoBuilder lastStateChange(LocalDateTime lastStateChange) {
        this.lastStateChange = lastStateChange;
        return this;
      }

      public CircuitBreakerMonitoringInfoBuilder stateChangeDuration(Duration stateChangeDuration) {
        this.stateChangeDuration = stateChangeDuration;
        return this;
      }

      public CircuitBreakerMonitoringInfoBuilder error(String error) {
        this.error = error;
        return this;
      }

      public CircuitBreakerMonitoringInfo build() {
        CircuitBreakerMonitoringInfo info = new CircuitBreakerMonitoringInfo();
        info.serviceName = this.serviceName;
        info.currentState = this.currentState;
        info.failureRate = this.failureRate;
        info.successfulCalls = this.successfulCalls;
        info.failedCalls = this.failedCalls;
        info.lastStateChange = this.lastStateChange;
        info.stateChangeDuration = this.stateChangeDuration;
        info.error = this.error;
        return info;
      }
    }
  }

  /** Monitoring summary for all circuit breakers. */
  public static class CircuitBreakerMonitoringSummary {
    private Map<String, CircuitBreakerMonitoringInfo> circuitBreakers;
    private long totalAlertsSent;
    private long circuitOpenEvents;
    private long circuitClosedEvents;
    private boolean monitoringEnabled;
    private LocalDateTime lastUpdate;

    public static CircuitBreakerMonitoringSummaryBuilder builder() {
      return new CircuitBreakerMonitoringSummaryBuilder();
    }

    // Getters and setters
    public Map<String, CircuitBreakerMonitoringInfo> getCircuitBreakers() {
      return circuitBreakers;
    }

    public void setCircuitBreakers(Map<String, CircuitBreakerMonitoringInfo> circuitBreakers) {
      this.circuitBreakers = circuitBreakers;
    }

    public long getTotalAlertsSent() {
      return totalAlertsSent;
    }

    public void setTotalAlertsSent(long totalAlertsSent) {
      this.totalAlertsSent = totalAlertsSent;
    }

    public long getCircuitOpenEvents() {
      return circuitOpenEvents;
    }

    public void setCircuitOpenEvents(long circuitOpenEvents) {
      this.circuitOpenEvents = circuitOpenEvents;
    }

    public long getCircuitClosedEvents() {
      return circuitClosedEvents;
    }

    public void setCircuitClosedEvents(long circuitClosedEvents) {
      this.circuitClosedEvents = circuitClosedEvents;
    }

    public boolean isMonitoringEnabled() {
      return monitoringEnabled;
    }

    public void setMonitoringEnabled(boolean monitoringEnabled) {
      this.monitoringEnabled = monitoringEnabled;
    }

    public LocalDateTime getLastUpdate() {
      return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
      this.lastUpdate = lastUpdate;
    }

    public static class CircuitBreakerMonitoringSummaryBuilder {
      private Map<String, CircuitBreakerMonitoringInfo> circuitBreakers;
      private long totalAlertsSent;
      private long circuitOpenEvents;
      private long circuitClosedEvents;
      private boolean monitoringEnabled;
      private LocalDateTime lastUpdate;

      public CircuitBreakerMonitoringSummaryBuilder circuitBreakers(
          Map<String, CircuitBreakerMonitoringInfo> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
        return this;
      }

      public CircuitBreakerMonitoringSummaryBuilder totalAlertsSent(long totalAlertsSent) {
        this.totalAlertsSent = totalAlertsSent;
        return this;
      }

      public CircuitBreakerMonitoringSummaryBuilder circuitOpenEvents(long circuitOpenEvents) {
        this.circuitOpenEvents = circuitOpenEvents;
        return this;
      }

      public CircuitBreakerMonitoringSummaryBuilder circuitClosedEvents(long circuitClosedEvents) {
        this.circuitClosedEvents = circuitClosedEvents;
        return this;
      }

      public CircuitBreakerMonitoringSummaryBuilder monitoringEnabled(boolean monitoringEnabled) {
        this.monitoringEnabled = monitoringEnabled;
        return this;
      }

      public CircuitBreakerMonitoringSummaryBuilder lastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
      }

      public CircuitBreakerMonitoringSummary build() {
        CircuitBreakerMonitoringSummary summary = new CircuitBreakerMonitoringSummary();
        summary.circuitBreakers = this.circuitBreakers;
        summary.totalAlertsSent = this.totalAlertsSent;
        summary.circuitOpenEvents = this.circuitOpenEvents;
        summary.circuitClosedEvents = this.circuitClosedEvents;
        summary.monitoringEnabled = this.monitoringEnabled;
        summary.lastUpdate = this.lastUpdate;
        return summary;
      }
    }
  }
}
