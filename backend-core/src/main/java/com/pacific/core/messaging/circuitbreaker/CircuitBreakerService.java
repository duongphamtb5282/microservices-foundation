package com.pacific.core.messaging.circuitbreaker;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Service for managing circuit breakers for external service calls. Uses Resilience4j for circuit
 * breaker implementation.
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

  /**
   * Execute operation with circuit breaker protection.
   *
   * @param serviceName Name of the external service
   * @param operation The operation to execute
   * @param <T> Return type
   * @return Operation result
   */
  public <T> T execute(String serviceName, Supplier<T> operation) {
    CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);

    Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, operation);

    try {
      T result = decoratedSupplier.get();
      log.debug("Circuit breaker call successful for service: {}", serviceName);
      return result;
    } catch (Exception e) {
      log.warn("Circuit breaker call failed for service: {}", serviceName, e);
      throw e;
    }
  }

  /**
   * Execute operation asynchronously with circuit breaker protection.
   *
   * @param serviceName Name of the external service
   * @param operation The operation to execute
   * @param <T> Return type
   * @return CompletableFuture with operation result
   */
  public <T> java.util.concurrent.CompletableFuture<T> executeAsync(
      String serviceName, Supplier<T> operation) {
    CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);

    Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, operation);

    return java.util.concurrent.CompletableFuture.supplyAsync(decoratedSupplier);
  }

  /** Get or create circuit breaker for service. */
  private CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
    return circuitBreakers.computeIfAbsent(serviceName, this::createCircuitBreaker);
  }

  /** Create circuit breaker with default configuration. */
  private CircuitBreaker createCircuitBreaker(String serviceName) {
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Open circuit if 50% of requests fail
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
            .slidingWindowSize(10) // Consider last 10 calls for failure rate
            .minimumNumberOfCalls(5) // Minimum calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .ignoreExceptions(java.net.ConnectException.class) // Ignore connection exceptions
            .build();

    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName, config);

    // Add event listeners for monitoring
    circuitBreaker
        .getEventPublisher()
        .onStateTransition(
            event ->
                log.info(
                    "Circuit breaker state changed: {} -> {} for service: {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState(),
                    serviceName))
        .onFailureRateExceeded(
            event -> log.warn("Circuit breaker failure rate exceeded for service: {}", serviceName))
        .onCallNotPermitted(
            event -> log.warn("Circuit breaker call not permitted for service: {}", serviceName));

    return circuitBreaker;
  }

  /** Get circuit breaker state for a service. */
  public CircuitBreaker.State getState(String serviceName) {
    CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
    return circuitBreaker != null ? circuitBreaker.getState() : CircuitBreaker.State.CLOSED;
  }

  /** Reset circuit breaker for a service. */
  public void reset(String serviceName) {
    CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
    if (circuitBreaker != null) {
      circuitBreaker.reset();
      log.info("Circuit breaker reset for service: {}", serviceName);
    }
  }

  /** Get circuit breaker metrics. */
  public CircuitBreakerMetrics getMetrics(String serviceName) {
    CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
    if (circuitBreaker == null) {
      return CircuitBreakerMetrics.empty();
    }

    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
    return CircuitBreakerMetrics.builder()
        .state(circuitBreaker.getState())
        .failureRate(metrics.getFailureRate())
        .numberOfSuccessfulCalls(metrics.getNumberOfSuccessfulCalls())
        .numberOfFailedCalls(metrics.getNumberOfFailedCalls())
        .numberOfBufferedCalls(metrics.getNumberOfBufferedCalls())
        .build();
  }

  /** Metrics for circuit breaker. */
  public static class CircuitBreakerMetrics {
    private final CircuitBreaker.State state;
    private final float failureRate;
    private final int numberOfSuccessfulCalls;
    private final int numberOfFailedCalls;
    private final int numberOfBufferedCalls;

    public CircuitBreakerMetrics(
        CircuitBreaker.State state,
        float failureRate,
        int numberOfSuccessfulCalls,
        int numberOfFailedCalls,
        int numberOfBufferedCalls) {
      this.state = state;
      this.failureRate = failureRate;
      this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
      this.numberOfFailedCalls = numberOfFailedCalls;
      this.numberOfBufferedCalls = numberOfBufferedCalls;
    }

    public static CircuitBreakerMetrics empty() {
      return new CircuitBreakerMetrics(CircuitBreaker.State.CLOSED, 0, 0, 0, 0);
    }

    // Getters
    public CircuitBreaker.State getState() {
      return state;
    }

    public float getFailureRate() {
      return failureRate;
    }

    public int getNumberOfSuccessfulCalls() {
      return numberOfSuccessfulCalls;
    }

    public int getNumberOfFailedCalls() {
      return numberOfFailedCalls;
    }

    public int getNumberOfBufferedCalls() {
      return numberOfBufferedCalls;
    }

    public static CircuitBreakerMetricsBuilder builder() {
      return new CircuitBreakerMetricsBuilder();
    }

    public static class CircuitBreakerMetricsBuilder {
      private CircuitBreaker.State state = CircuitBreaker.State.CLOSED;
      private float failureRate = 0;
      private int numberOfSuccessfulCalls = 0;
      private int numberOfFailedCalls = 0;
      private int numberOfBufferedCalls = 0;

      public CircuitBreakerMetricsBuilder state(CircuitBreaker.State state) {
        this.state = state;
        return this;
      }

      public CircuitBreakerMetricsBuilder failureRate(float failureRate) {
        this.failureRate = failureRate;
        return this;
      }

      public CircuitBreakerMetricsBuilder numberOfSuccessfulCalls(int numberOfSuccessfulCalls) {
        this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
        return this;
      }

      public CircuitBreakerMetricsBuilder numberOfFailedCalls(int numberOfFailedCalls) {
        this.numberOfFailedCalls = numberOfFailedCalls;
        return this;
      }

      public CircuitBreakerMetricsBuilder numberOfBufferedCalls(int numberOfBufferedCalls) {
        this.numberOfBufferedCalls = numberOfBufferedCalls;
        return this;
      }

      public CircuitBreakerMetrics build() {
        return new CircuitBreakerMetrics(
            state,
            failureRate,
            numberOfSuccessfulCalls,
            numberOfFailedCalls,
            numberOfBufferedCalls);
      }
    }
  }
}
