package com.pacific.payment.modules.payment.exception;

/**
 * Raised when a payment row that should exist cannot be found (e.g. the idempotency pre-check found
 * a payment for an order but the row is gone — data corruption). Fail loudly instead of replaying a
 * phantom payment.
 */
public class PaymentNotFoundException extends RuntimeException {

  public PaymentNotFoundException(String message) {
    super(message);
  }
}
