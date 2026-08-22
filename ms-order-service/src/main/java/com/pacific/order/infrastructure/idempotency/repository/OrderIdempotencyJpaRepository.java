package com.pacific.order.infrastructure.idempotency.repository;

import com.pacific.order.infrastructure.idempotency.entity.OrderIdempotencyEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA storage for client idempotency keys (docs/architecture/idempotency-proposal.md). */
public interface OrderIdempotencyJpaRepository
    extends JpaRepository<OrderIdempotencyEntity, OrderIdempotencyEntity.OrderIdempotencyKey> {

  Optional<OrderIdempotencyEntity> findByUserIdAndIdempotencyKey(
      String userId, String idempotencyKey);
}
