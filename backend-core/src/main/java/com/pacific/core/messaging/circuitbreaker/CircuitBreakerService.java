package com.pacific.core.messaging.circuitbreaker;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;

/**
 * Service for managing circuit breakers for external service calls. Uses Resilience4j for circuit
 * breaker implementation.
 */
@Service
// Single definition point (ADR-0011): no manual @Bean in BackendCoreConfiguration.
// Gated on the resilience4j classpath: consumers without the dependency would crash at bean
// creation while introspecting this class (NoClassDefFoundError: CircuitBreaker$State). The
// fileTree jar dependency carries no transitive metadata, so core's `api` resilience4j deps don't
// flow to consumers — each must declare them (auth, order, customer, gateway) or skip the bean
// (ms-payment, which injects nothing from this class).
@ConditionalOnClass({CircuitBreakerRegistry.class, CircuitBreaker.class})
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {

  private final CircuitBreakerRegistry circuitBreakerRegistry;

  @Qualifier("coreAsyncExecutor")
  private final Executor coreAsyncExecutor;

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

    // No catch-log-rethrow (ADR-0012): the failure propagates with its own context; the caller
    // decides the fallback. Resilience4j translates an open breaker into CallNotPermittedException.
    T result = decoratedSupplier.get();
    log.debug("Circuit breaker call successful for service: {}", serviceName);
    return result;
  }

  /**
   * Execute operation asynchronously with circuit breaker protection.
   *
   * @param serviceName Name of the external service
   * @param operation The operation to execute
   * @param <T> Return type
   * @return CompletableFuture with operation result
   */
  public <T> CompletableFuture<T> executeAsync(String serviceName, Supplier<T> operation) {
    CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);

    Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, operation);

    // ADR-0011: run on the shared core-async executor (virtual threads) — never the common pool.
    return CompletableFuture.supplyAsync(decoratedSupplier, coreAsyncExecutor);
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
