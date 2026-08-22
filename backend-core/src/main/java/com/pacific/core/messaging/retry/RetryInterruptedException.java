package com.pacific.core.messaging.retry;

/**
 * Thrown when a retry operation is interrupted while sleeping through backoff. Carries the
 * {@link InterruptedException} as cause; the caller must re-check the interrupt flag.
 */
public class RetryInterruptedException extends RuntimeException {

  public RetryInterruptedException(String message, InterruptedException cause) {
    super(message, cause);
  }
}
