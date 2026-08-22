package com.pacific.payment.modules.consumer.exception;

/**
 * Raised from the consumer when an event failed with a retryable error after in-listener retries.
 * Propagating (no ack) makes Kafka redeliver the message — at-least-once beats loss.
 */
public class EventRetryableException extends RuntimeException {

  public EventRetryableException(String message, Throwable cause) {
    super(message, cause);
  }
}
