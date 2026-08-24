package com.pacific.core.messaging.retry.impl;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.error.ErrorClassifier;
import com.pacific.core.messaging.retry.BackoffStrategy;
import com.pacific.core.messaging.retry.MaxRetriesExceededException;
import com.pacific.core.messaging.retry.RetryContext;
import com.pacific.core.messaging.retry.RetryInterruptedException;
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

  @Qualifier("coreAsyncExecutor")
  private final Executor coreAsyncExecutor;

  @Override
  public <T> T executeWithRetry(Callable<T> operation, RetryPolicy policy, RetryContext context)
      throws Exception {

    while (true) {
      try {
        context.incrementAttempt();

        log.debug(
            "Executing operation, attempt: {}/{} (messageId: {})",
            context.getAttemptNumber(),
            policy.getMaxAttempts(),
            context.getMessageId());

        T result = operation.call();

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
          // Cap the blocking backoff sleep at 5s so a retry loop can never block the consumer
          // thread for the full (potentially unbounded) backoff duration (F-21).
          long sleepMillis = Math.min(backoff.toMillis(), 5000L);
          log.info(
              "Backing off for {}ms before retry attempt {} (messageId: {})",
              sleepMillis,
              context.getAttemptNumber() + 1,
              context.getMessageId());

          Thread.sleep(sleepMillis);

        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.error("Retry interrupted (messageId: {})", context.getMessageId());
          throw new RetryInterruptedException("Retry interrupted", ie);
        }
      }
    }
  }

  @Override
  public <T> CompletableFuture<T> executeWithRetryAsync(
      Callable<T> operation, RetryPolicy policy, RetryContext context) {

    // ADR-0011: run on the shared core-async executor (virtual threads) — never the common pool.
    // executeWithRetry can rethrow the operation's original checked exception (non-retryable
    // path); wrap it in CompletionException, mirroring what supplyAsync does natively so the
    // future still completes exceptionally.
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return executeWithRetry(operation, policy, context);
          } catch (Exception e) {
            throw new CompletionException(e);
          }
        },
        coreAsyncExecutor);
  }
}
