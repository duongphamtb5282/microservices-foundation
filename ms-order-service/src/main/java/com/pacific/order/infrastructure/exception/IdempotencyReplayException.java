package com.pacific.order.infrastructure.exception;

/**
 * Raised when an idempotency key maps to a missing order (idempotency-proposal.md). The row is
 * written in the same transaction as the order, so this indicates data corruption — fail loudly
 * instead of replaying a phantom order.
 */
public class IdempotencyReplayException extends RuntimeException {

  public IdempotencyReplayException(String message) {
    super(message);
  }
}
