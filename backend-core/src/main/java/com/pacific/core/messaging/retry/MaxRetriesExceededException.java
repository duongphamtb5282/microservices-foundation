package com.pacific.core.messaging.retry;

/** Exception thrown when maximum retry attempts have been exceeded. */
public class MaxRetriesExceededException extends RuntimeException {

  public MaxRetriesExceededException(String message) {
    super(message);
  }

  public MaxRetriesExceededException(String message, Throwable cause) {
    super(message, cause);
  }
}
