package com.pacific.auth.common.exception;

/** Thrown when an outbox event cannot be serialized to JSON (ADR-0006). */
public class OutboxSerializationException extends RuntimeException {

  public OutboxSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
