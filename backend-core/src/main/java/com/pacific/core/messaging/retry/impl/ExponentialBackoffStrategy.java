package com.pacific.core.messaging.retry.impl;

import java.time.Duration;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.retry.BackoffStrategy;
import com.pacific.core.messaging.retry.RetryPolicy;

/**
 * Exponential backoff strategy with jitter.
 *
 * <p>Formula: min(maxBackoff, initialBackoff * (multiplier ^ (attempt - 1))) + jitter
 *
 * <p>Example with defaults (initialBackoff=1s, multiplier=2.0, jitter=10%): - Attempt 1: 1s +
 * (0-100ms) = ~1s - Attempt 2: 2s + (0-200ms) = ~2s - Attempt 3: 4s + (0-400ms) = ~4s
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ExponentialBackoffStrategy implements BackoffStrategy {

  private final Random random = new Random();

  @Override
  public Duration calculateBackoff(int attempt, RetryPolicy policy) {
    if (attempt <= 0) {
      return Duration.ZERO;
    }

    // Calculate base exponential backoff
    long baseBackoffMs = policy.getInitialBackoff().toMillis();
    double multiplier = policy.getBackoffMultiplier();

    // Exponential: initialBackoff * (multiplier ^ (attempt - 1))
    long exponentialBackoffMs = (long) (baseBackoffMs * Math.pow(multiplier, attempt - 1));

    // Cap at max backoff
    long cappedBackoffMs = Math.min(exponentialBackoffMs, policy.getMaxBackoff().toMillis());

    // Add jitter to prevent thundering herd
    double jitterFactor = policy.getJitterFactor();
    long jitterMs = (long) (cappedBackoffMs * jitterFactor * random.nextDouble());

    long finalBackoffMs = cappedBackoffMs + jitterMs;

    log.debug(
        "Calculated backoff for attempt {}: {}ms (base: {}ms, jitter: {}ms)",
        attempt,
        finalBackoffMs,
        cappedBackoffMs,
        jitterMs);

    return Duration.ofMillis(finalBackoffMs);
  }
}
