package com.pacific.order.infrastructure.exception;

/**
 * Raised when an outbox event payload cannot be serialized. A serialization failure must roll back
 * the enclosing transaction (order + outbox row stay consistent) rather than being swallowed.
 */
public class OutboxSerializationException extends RuntimeException {

  public OutboxSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
