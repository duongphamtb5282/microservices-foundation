package com.pacific.core.messaging.retry;

import java.time.Duration;
import java.util.List;

import lombok.Builder;
import lombok.Value;

/**
 * Configuration for retry behavior. Defines how many times to retry, backoff strategy, and which
 * exceptions to retry.
 */
@Value
@Builder
public class RetryPolicy {

  /** Maximum number of retry attempts (including initial attempt). Default: 3 attempts */
  @Builder.Default int maxAttempts = 3;

  /** Initial backoff duration before first retry. Default: 1 second */
  @Builder.Default Duration initialBackoff = Duration.ofSeconds(1);

  /** Maximum backoff duration to cap exponential growth. Default: 5 minutes */
  @Builder.Default Duration maxBackoff = Duration.ofMinutes(5);

  /**
   * Backoff multiplier for exponential backoff. Each retry waits: initialBackoff * (multiplier ^
   * attemptNumber) Default: 2.0 (double the wait time each attempt)
   */
  @Builder.Default double backoffMultiplier = 2.0;

  /**
   * Jitter factor (0.0 to 1.0) for randomization. Adds random variance to prevent thundering herd.
   * Default: 0.1 (10% randomization)
   */
  @Builder.Default double jitterFactor = 0.1;

  /** Exceptions that should trigger retry (if not empty, only these will retry). */
  List<Class<? extends Exception>> retryableExceptions;

  /** Exceptions that should NOT trigger retry. */
  List<Class<? extends Exception>> nonRetryableExceptions;

  /** Enable Dead Letter Queue for failed messages after max retries. Default: true */
  @Builder.Default boolean enableDlq = true;

  /**
   * Create a default retry policy.
   *
   * @return Default RetryPolicy
   */
  public static RetryPolicy defaultPolicy() {
    return RetryPolicy.builder().build();
  }
}
