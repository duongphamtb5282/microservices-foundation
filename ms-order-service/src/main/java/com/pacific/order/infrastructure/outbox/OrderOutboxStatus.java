package com.pacific.order.infrastructure.outbox;

/** Outbox row lifecycle status (ADR-0001). */
public enum OrderOutboxStatus {
  PENDING,
  PUBLISHED,
  FAILED
}
