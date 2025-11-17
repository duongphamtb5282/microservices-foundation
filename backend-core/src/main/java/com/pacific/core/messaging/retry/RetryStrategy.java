package com.pacific.core.messaging.retry;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Strategy for executing operations with retry logic. */
public interface RetryStrategy {

  /**
   * Execute operation with retry logic synchronously.
   *
   * @param operation The operation to execute
   * @param policy The retry policy
   * @param context The retry context
   * @param <T> Result type
   * @return Operation result
   * @throws MaxRetriesExceededException if max retries exceeded
   */
  <T> T executeWithRetry(Supplier<T> operation, RetryPolicy policy, RetryContext context);

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
      Supplier<T> operation, RetryPolicy policy, RetryContext context);
}
