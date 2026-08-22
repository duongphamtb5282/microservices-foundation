package com.pacific.order.infrastructure.idempotency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client idempotency keys for order creation (docs/architecture/idempotency-proposal.md). The
 * composite PK (user_id, idempotency_key) is the concurrency backstop: two simultaneous requests
 * with the same key cannot both insert. Written inside the same transaction as the order + outbox
 * row, so a failed order write leaves no phantom key behind.
 */
@Data
@Entity
@Table(name = "order_idempotency_keys")
@IdClass(OrderIdempotencyEntity.OrderIdempotencyKey.class)
public class OrderIdempotencyEntity {

  @Id
  @Column(name = "idempotency_key", length = 128, nullable = false)
  private String idempotencyKey;

  @Id
  @Column(name = "user_id", length = 36, nullable = false)
  private String userId;

  @Column(name = "order_id", length = 36, nullable = false)
  private String orderId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public static OrderIdempotencyEntity of(String idempotencyKey, String userId, String orderId) {
    OrderIdempotencyEntity entity = new OrderIdempotencyEntity();
    entity.setIdempotencyKey(idempotencyKey);
    entity.setUserId(userId);
    entity.setOrderId(orderId);
    entity.setCreatedAt(Instant.now());
    return entity;
  }

  /** Composite key class for @IdClass mapping. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderIdempotencyKey implements Serializable {
    private String idempotencyKey;
    private String userId;
  }
}
