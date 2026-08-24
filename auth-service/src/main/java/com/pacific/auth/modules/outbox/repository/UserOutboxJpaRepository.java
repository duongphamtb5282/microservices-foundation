package com.pacific.auth.modules.outbox.repository;

import com.pacific.auth.modules.outbox.UserOutboxStatus;
import com.pacific.auth.modules.outbox.entity.UserOutboxEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for the transactional outbox (ADR-0006). */
public interface UserOutboxJpaRepository extends JpaRepository<UserOutboxEntity, String> {

  /** Pending rows in insertion order — the relay poll query. */
  List<UserOutboxEntity> findByStatusOrderByCreatedAtAsc(
      UserOutboxStatus status, Pageable pageable);

  /** Cleanup: purge PUBLISHED rows published before the retention cutoff. */
  @Transactional
  long deleteByStatusAndPublishedAtBefore(UserOutboxStatus status, Instant publishedAt);
}
