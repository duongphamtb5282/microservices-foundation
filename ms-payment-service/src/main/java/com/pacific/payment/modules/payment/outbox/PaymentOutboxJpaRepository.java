package com.pacific.payment.modules.payment.outbox;

import com.pacific.payment.modules.payment.outbox.entity.PaymentOutboxEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for the payment result outbox (F-02). */
public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxEntity, String> {

  /** Pending rows in insertion order — the relay poll query. */
  List<PaymentOutboxEntity> findByStatusOrderByCreatedAtAsc(
      PaymentOutboxStatus status, Pageable pageable);

  /** Cleanup: purge PUBLISHED rows published before the retention cutoff. */
  @Transactional
  long deleteByStatusAndPublishedAtBefore(PaymentOutboxStatus status, Instant publishedAt);
}
