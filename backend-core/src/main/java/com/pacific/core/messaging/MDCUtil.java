package com.pacific.core.messaging;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.MDC;

public class MDCUtil {

  /**
   * Execute a supplier with the current MDC context propagated to async execution
   *
   * @param supplier The task to execute asynchronously
   * @param <T> Return type
   * @return CompletableFuture with the result
   */
  public static <T> CompletableFuture<T> executeAsyncWithMdc(Supplier<T> supplier) {
    // Capture current MDC context
    Map<String, String> contextMap = MDC.getCopyOfContextMap();

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Restore MDC context in child thread
            if (contextMap != null && !contextMap.isEmpty()) {
              contextMap.forEach(
                  (key, value) -> {
                    if (key != null && value != null) {
                      MDC.put(key, value);
                    }
                  });
            }

            // Execute the supplier
            return supplier.get();

          } finally {
            // Always clean up MDC in child thread to prevent memory leaks
            MDC.clear();
          }
        });
  }

  /**
   * Execute a callable with MDC context preservation
   *
   * @param callable The task to execute
   * @param <T> Return type
   * @return The result of the callable
   * @throws Exception Any exception thrown by the callable
   */
  public static <T> T executeWithMdcContext(Callable<T> callable) throws Exception {
    // Capture current MDC context
    Map<String, String> contextMap = MDC.getCopyOfContextMap();

    try {
      // Temporarily store current context if needed
      // But for synchronous execution, just execute directly

      // Execute the callable (MDC context is already available in current thread)
      return callable.call();

    } finally {
      // No cleanup needed for synchronous execution - context is thread-local
      // But ensure any child threads clean up their own context
    }
  }

  /**
   * Execute a runnable asynchronously with MDC context propagation
   *
   * @param runnable The task to execute asynchronously
   */
  public static void executeAsyncWithMdc(Runnable runnable) {
    // Capture current MDC context
    Map<String, String> contextMap = MDC.getCopyOfContextMap();

    CompletableFuture.runAsync(
        () -> {
          try {
            // Restore MDC context in child thread
            if (contextMap != null && !contextMap.isEmpty()) {
              contextMap.forEach(
                  (key, value) -> {
                    if (key != null && value != null) {
                      MDC.put(key, value);
                    }
                  });
            }

            // Execute the runnable
            runnable.run();

          } finally {
            // Clean up MDC in child thread
            MDC.clear();
          }
        });
  }

  /**
   * Safely get correlation ID from MDC with fallback
   *
   * @return Correlation ID or generated UUID if missing
   */
  public static String getCorrelationId() {
    String correlationId = MDC.get("correlationId");
    if (correlationId == null || correlationId.trim().isEmpty()) {
      correlationId = java.util.UUID.randomUUID().toString();
      MDC.put("correlationId", correlationId);
    }
    return correlationId;
  }

  /**
   * Safely set correlation ID in MDC
   *
   * @param correlationId The correlation ID to set
   */
  public static void setCorrelationId(String correlationId) {
    if (correlationId != null && !correlationId.trim().isEmpty()) {
      MDC.put("correlationId", correlationId.trim());
    } else {
      // Generate if null/empty
      String generatedId = java.util.UUID.randomUUID().toString();
      MDC.put("correlationId", generatedId);
    }
  }

  /** Clear all MDC context safely */
  public static void clearContext() {
    MDC.clear();
  }

  /**
   * Check if MDC contains correlation ID
   *
   * @return true if correlation ID exists and is non-empty
   */
  public static boolean hasCorrelationId() {
    String correlationId = MDC.get("correlationId");
    return correlationId != null && !correlationId.trim().isEmpty();
  }

  /**
   * Propagate MDC context to a child thread manually
   *
   * @param contextMap The MDC context map to propagate
   */
  public static void propagateContext(Map<String, String> contextMap) {
    if (contextMap != null && !contextMap.isEmpty()) {
      contextMap.forEach(
          (key, value) -> {
            if (key != null && value != null) {
              MDC.put(key, value);
            }
          });
    }
  }
}
