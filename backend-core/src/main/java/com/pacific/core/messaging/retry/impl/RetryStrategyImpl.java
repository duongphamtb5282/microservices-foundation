package com.pacific.core.messaging.retry.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.error.ErrorClassifier;
import com.pacific.core.messaging.retry.BackoffStrategy;
import com.pacific.core.messaging.retry.MaxRetriesExceededException;
import com.pacific.core.messaging.retry.RetryContext;
import com.pacific.core.messaging.retry.RetryPolicy;
import com.pacific.core.messaging.retry.RetryStrategy;

/** Implementation of retry strategy with exponential backoff and error classification. */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class RetryStrategyImpl implements RetryStrategy {

  private final BackoffStrategy backoffStrategy;
  private final ErrorClassifier errorClassifier;
  private final DeadLetterQueue deadLetterQueue;

  @Override
  public <T> T executeWithRetry(Supplier<T> operation, RetryPolicy policy, RetryContext context) {

    while (true) {
      try {
        context.incrementAttempt();

        log.debug(
            "Executing operation, attempt: {}/{} (messageId: {})",
            context.getAttemptNumber(),
            policy.getMaxAttempts(),
            context.getMessageId());

        T result = operation.get();

        if (context.getAttemptNumber() > 1) {
          log.info(
              "Operation succeeded after {} attempts (messageId: {})",
              context.getAttemptNumber(),
              context.getMessageId());
        }

        return result;

      } catch (Exception e) {
        context.recordException(e);

        log.warn(
            "Operation failed on attempt {}/{} (messageId: {}): {}",
            context.getAttemptNumber(),
            policy.getMaxAttempts(),
            context.getMessageId(),
            e.getMessage());

        // Check if exception is retryable
        if (!errorClassifier.isRetryable(e, policy)) {
          log.error(
              "Non-retryable exception occurred, failing immediately (messageId: {})",
              context.getMessageId(),
              e);

          if (policy.isEnableDlq()) {
            deadLetterQueue.send(context, e);
          }

          throw e;
        }

        // Check if we should retry
        if (!context.shouldRetry(policy)) {
          log.error(
              "Max retry attempts ({}) reached for messageId: {}, sending to DLQ",
              policy.getMaxAttempts(),
              context.getMessageId());

          if (policy.isEnableDlq()) {
            deadLetterQueue.send(context, e);
          }

          throw new MaxRetriesExceededException(
              "Failed after " + context.getAttemptNumber() + " attempts", e);
        }

        // Calculate and apply backoff
        Duration backoff = backoffStrategy.calculateBackoff(context.getAttemptNumber(), policy);

        try {
          log.info(
              "Backing off for {}ms before retry attempt {} (messageId: {})",
              backoff.toMillis(),
              context.getAttemptNumber() + 1,
              context.getMessageId());

          Thread.sleep(backoff.toMillis());

        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.error("Retry interrupted (messageId: {})", context.getMessageId());
          throw new RuntimeException("Retry interrupted", ie);
        }
      }
    }
  }

  @Override
  public <T> CompletableFuture<T> executeWithRetryAsync(
      Supplier<T> operation, RetryPolicy policy, RetryContext context) {

    return CompletableFuture.supplyAsync(() -> executeWithRetry(operation, policy, context));
  }
}
