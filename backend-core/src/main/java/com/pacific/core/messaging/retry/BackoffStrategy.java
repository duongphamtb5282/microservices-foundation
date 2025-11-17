package com.pacific.core.messaging.retry;

import java.time.Duration;

/**
 * Strategy for calculating backoff delays between retry attempts. Implementations can provide
 * different algorithms (exponential, linear, fixed, etc.).
 */
public interface BackoffStrategy {

  /**
   * Calculate backoff duration for given attempt.
   *
   * @param attempt Current retry attempt number (1-based)
   * @param policy Retry policy configuration
   * @return Duration to wait before next retry
   */
  Duration calculateBackoff(int attempt, RetryPolicy policy);
}
