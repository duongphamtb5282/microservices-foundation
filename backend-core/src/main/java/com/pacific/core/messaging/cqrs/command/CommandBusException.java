package com.pacific.core.messaging.cqrs.command;

/**
 * Thrown when a command cannot be dispatched: duplicate handler registration or missing handler.
 */
public class CommandBusException extends RuntimeException {

  public CommandBusException(String message) {
    super(message);
  }

  public CommandBusException(String message, Throwable cause) {
    super(message, cause);
  }
}
