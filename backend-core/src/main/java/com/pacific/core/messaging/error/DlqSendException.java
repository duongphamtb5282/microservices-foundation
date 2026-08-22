package com.pacific.core.messaging.error;

/**
 * Thrown when a message cannot be sent to the Dead Letter Queue. Propagated so the caller does not
 * acknowledge the message, allowing redelivery (at-least-once beats loss) (6c).
 */
public class DlqSendException extends RuntimeException {

  public DlqSendException(String message, Throwable cause) {
    super(message, cause);
  }
}
