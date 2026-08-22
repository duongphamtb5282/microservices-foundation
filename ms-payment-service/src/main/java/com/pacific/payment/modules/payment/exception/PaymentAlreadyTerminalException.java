package com.pacific.payment.modules.payment.exception;

/**
 * Raised when a payment transitions from a terminal state (domain invariant violation). Typed so
 * callers can distinguish this from generic infrastructure failures.
 */
public class PaymentAlreadyTerminalException extends RuntimeException {

  public PaymentAlreadyTerminalException(String message) {
    super(message);
  }
}
