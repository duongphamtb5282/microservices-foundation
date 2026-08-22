package com.pacific.order.infrastructure.exception;

/**
 * Raised when the event store fails to read or write — a corrupt/unavailable event store must never
 * be masked as "no events" or "aggregate not found" (review finding: read failures previously
 * returned empty results, causing silent replay-from-scratch and data divergence).
 */
public class EventStoreException extends RuntimeException {

  public EventStoreException(String message) {
    super(message);
  }

  public EventStoreException(String message, Throwable cause) {
    super(message, cause);
  }
}
