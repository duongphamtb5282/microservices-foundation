package com.pacific.core.messaging.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy for executing operations with retry logic.
 *
 * <p>The operation is a {@link Callable}, not a {@link java.util.function.Supplier}: retried
 * operations (Kafka event processing, outbound calls) routinely throw checked exceptions, and the
 * retry machinery must classify the original exception by type. A Supplier would force callers to
 * wrap checked exceptions in runtime types, defeating type-based classification (TO-0010).
 */
public interface RetryStrategy {

  /**
   * Execute operation with retry logic synchronously.
   *
   * @param operation The operation to execute
   * @param policy The retry policy
   * @param context The retry context
   * @param <T> Result type
   * @return Operation result
   * @throws Exception the operation's original exception, unchanged, when it is classified as
   *     non-retryable (and DLQ is disabled) — callers re-classify it, so the type must survive
   * @throws MaxRetriesExceededException if max retries exceeded
   */
  <T> T executeWithRetry(Callable<T> operation, RetryPolicy policy, RetryContext context)
      throws Exception;

  /**
   * Execute operation with retry logic asynchronously.
   *
   * @param operation The operation to execute
   * @param policy The retry policy
   * @param context The retry context
   * @param <T> Result type
   * @return CompletableFuture with operation result
   */
  <T> CompletableFuture<T> executeWithRetryAsync(
      Callable<T> operation, RetryPolicy policy, RetryContext context);
}
