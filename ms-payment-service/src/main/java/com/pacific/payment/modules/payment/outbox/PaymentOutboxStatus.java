package com.pacific.payment.modules.payment.outbox;

/** Lifecycle of a payment outbox row (ADR-0001 pattern, saga return path F-02). */
public enum PaymentOutboxStatus {
  /** Row written in the payment transaction, not yet acknowledged by the broker. */
  PENDING,
  /** Broker acknowledged the publish — row is done and eligible for cleanup. */
  PUBLISHED,
  /** Publish failed permanently (max attempts) — manual inspection required. */
  FAILED
}
