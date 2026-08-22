package com.pacific.payment.modules.payment.outbox;

/**
 * Raised when a payment outbox payload cannot be serialized. Propagates out of the payment
 * transaction so the whole payment + outbox write rolls back together.
 */
public class PaymentOutboxSerializationException extends RuntimeException {

  public PaymentOutboxSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
