package com.pacific.core.messaging.error;

import com.pacific.core.messaging.retry.RetryPolicy;

/**
 * Classifies exceptions to determine if they are retryable.
 *
 * <p>Categories: - TRANSIENT: Temporary errors that may resolve (network, timeout) - PERMANENT:
 * Errors that will never succeed (validation, business logic) - UNKNOWN: Uncategorized errors
 * (default to transient for safety)
 */
public interface ErrorClassifier {

  /**
   * Check if exception should trigger retry.
   *
   * @param exception The exception to classify
   * @param policy The retry policy with exception lists
   * @return true if should retry, false otherwise
   */
  boolean isRetryable(Throwable exception, RetryPolicy policy);

  /**
   * Get error category for exception.
   *
   * @param exception The exception to categorize
   * @return Error category
   */
  ErrorCategory categorize(Throwable exception);

  /** Error categories for classification */
  enum ErrorCategory {
    /** Temporary errors that may resolve with retry */
    TRANSIENT,

    /** Permanent errors that will never succeed */
    PERMANENT,

    /** Unknown/uncategorized errors */
    UNKNOWN
  }
}
