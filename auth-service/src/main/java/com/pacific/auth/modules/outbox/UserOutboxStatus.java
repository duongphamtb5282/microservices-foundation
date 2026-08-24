package com.pacific.auth.modules.outbox;

/** Outbox row lifecycle status (ADR-0006). */
public enum UserOutboxStatus {
  PENDING,
  PUBLISHED,
  FAILED
}
