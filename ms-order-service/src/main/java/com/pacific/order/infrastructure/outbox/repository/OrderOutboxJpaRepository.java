package com.pacific.order.infrastructure.outbox.repository;

import com.pacific.order.infrastructure.outbox.OrderOutboxStatus;
import com.pacific.order.infrastructure.outbox.entity.OrderOutboxEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for the transactional outbox (ADR-0001). */
public interface OrderOutboxJpaRepository extends JpaRepository<OrderOutboxEntity, String> {

  /** Pending rows in insertion order — the relay poll query. */
  List<OrderOutboxEntity> findByStatusOrderByCreatedAtAsc(
      OrderOutboxStatus status, Pageable pageable);

  /** Cleanup: purge PUBLISHED rows published before the retention cutoff. */
  @Transactional
  long deleteByStatusAndPublishedAtBefore(OrderOutboxStatus status, Instant publishedAt);
}
