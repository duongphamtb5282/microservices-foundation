package com.pacific.shared.exceptions;

/**
 * Thrown when a consumed event fails to process. Rethrown so the messaging framework's retry/error
 * handling applies instead of silently dropping the event (10).
 */
public class EventProcessingException extends RuntimeException {

  public EventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
