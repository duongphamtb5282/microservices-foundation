package com.pacific.core.messaging.error.impl;

import java.sql.SQLException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.NetworkException;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.error.ErrorClassifier;
import com.pacific.core.messaging.retry.RetryPolicy;

/**
 * Classifies exceptions as retryable or non-retryable.
 *
 * <p>Classification rules: 1. Check policy's explicit non-retryable list first 2. Check policy's
 * explicit retryable list second 3. Fall back to default classification
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class RetryableErrorClassifier implements ErrorClassifier {

  @Override
  public boolean isRetryable(Throwable exception, RetryPolicy policy) {
    // Check explicit non-retryable exceptions first
    if (isInList(exception, policy.getNonRetryableExceptions())) {
      log.debug("Exception {} is explicitly non-retryable", exception.getClass().getName());
      return false;
    }

    // Check explicit retryable exceptions
    if (policy.getRetryableExceptions() != null && !policy.getRetryableExceptions().isEmpty()) {
      boolean retryable = isInList(exception, policy.getRetryableExceptions());
      log.debug(
          "Exception {} is {} in retryable list",
          exception.getClass().getName(),
          retryable ? "found" : "not found");
      return retryable;
    }

    // Default classification based on exception type
    ErrorCategory category = categorize(exception);
    boolean retryable = category == ErrorCategory.TRANSIENT || category == ErrorCategory.UNKNOWN;

    log.debug(
        "Exception {} categorized as {} (retryable: {})",
        exception.getClass().getName(),
        category,
        retryable);

    return retryable;
  }

  @Override
  public ErrorCategory categorize(Throwable exception) {
    // Transient errors - should retry
    if (isTransientError(exception)) {
      return ErrorCategory.TRANSIENT;
    }

    // Permanent errors - should NOT retry
    if (isPermanentError(exception)) {
      return ErrorCategory.PERMANENT;
    }

    // Unknown - default to transient for safety (conservative approach)
    log.warn("Unknown exception type {}, treating as transient", exception.getClass().getName());
    return ErrorCategory.UNKNOWN;
  }

  /** Check if exception is a transient error. */
  private boolean isTransientError(Throwable exception) {
    // Network and timeout exceptions
    if (exception instanceof NetworkException
        || exception instanceof TimeoutException
        || exception instanceof java.net.SocketTimeoutException
        || exception instanceof java.net.ConnectException) {
      return true;
    }

    // Database connection issues
    if (exception instanceof SQLException) {
      String message = exception.getMessage().toLowerCase();
      return message.contains("timeout")
          || message.contains("connection")
          || message.contains("socket");
    }

    // Check exception message for transient indicators
    String message = exception.getMessage();
    if (message != null) {
      String lowerMessage = message.toLowerCase();
      return lowerMessage.contains("timeout")
          || lowerMessage.contains("connection")
          || lowerMessage.contains("unavailable")
          || lowerMessage.contains("temporarily")
          || lowerMessage.contains("retry");
    }

    return false;
  }

  /** Check if exception is a permanent error. */
  private boolean isPermanentError(Throwable exception) {
    // Validation and serialization errors
    if (exception instanceof DeserializationException
        || exception instanceof IllegalArgumentException
        || exception instanceof NullPointerException
        || exception instanceof IllegalStateException) {
      return true;
    }

    // Check exception message for permanent indicators
    String message = exception.getMessage();
    if (message != null) {
      String lowerMessage = message.toLowerCase();
      return lowerMessage.contains("validation")
          || lowerMessage.contains("invalid")
          || lowerMessage.contains("malformed")
          || lowerMessage.contains("not found")
          || lowerMessage.contains("duplicate");
    }

    return false;
  }

  /** Check if exception is in the given list. */
  private boolean isInList(Throwable exception, List<Class<? extends Exception>> exceptionList) {
    if (exceptionList == null || exceptionList.isEmpty()) {
      return false;
    }

    return exceptionList.stream()
        .anyMatch(exClass -> exClass.isAssignableFrom(exception.getClass()));
  }
}
