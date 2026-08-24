package com.pacific.auth.modules.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.auth.modules.outbox.UserOutboxStatus;
import com.pacific.auth.modules.outbox.entity.UserOutboxEntity;
import com.pacific.auth.modules.outbox.repository.UserOutboxJpaRepository;
import com.pacific.core.messaging.cqrs.event.EventPublisher;
import com.pacific.shared.events.UserCreatedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Read side of the transactional outbox (ADR-0006). Polls PENDING rows and publishes them to Kafka,
 * marking each row PUBLISHED only after the send succeeds; rows that exhaust {@code
 * outbox.max-attempts} become FAILED for manual inspection. At-least-once delivery is safe because
 * the consumer (ms-customer) deduplicates on email and the unique event_id prevents duplicate rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserOutboxRelay {

  private static final String USER_EVENTS_TOPIC = "user-events";

  private final UserOutboxJpaRepository outboxRepository;
  private final EventPublisher eventPublisher;
  private final ObjectMapper objectMapper;
  @Qualifier("outboxPublisherExecutor")
  private final Executor outboxPublisherExecutor;

  @Value("${outbox.batch-size:100}")
  private int batchSize;

  @Value("${outbox.max-attempts:5}")
  private int maxAttempts;

  @Value("${outbox.retention-days:7}")
  private long retentionDays;

  @Scheduled(fixedDelayString = "${outbox.poll-interval:PT0.5S}")
  public void publishPendingEvents() {
    List<UserOutboxEntity> pending =
        outboxRepository.findByStatusOrderByCreatedAtAsc(
            UserOutboxStatus.PENDING, PageRequest.of(0, batchSize));

    if (pending.isEmpty()) {
      return;
    }

    // ADR-0011: publish the batch concurrently on the shared outbox-publish executor (4 threads),
    // then join so the next poll never overlaps this batch's DB writes. Per-row ordering is
    // preserved because each row is published as one task on a deterministic Kafka key.
    List<CompletableFuture<Void>> futures =
        pending.stream()
            .map(row -> CompletableFuture.runAsync(() -> publishRow(row), outboxPublisherExecutor))
            .toList();
    futures.forEach(CompletableFuture::join);
  }

  private void publishRow(UserOutboxEntity row) {
    try {
      UserCreatedEvent event = objectMapper.readValue(row.getPayload(), UserCreatedEvent.class);
      eventPublisher
          .publish(USER_EVENTS_TOPIC, row.getAggregateId(), event)
          .get(10, TimeUnit.SECONDS);
      row.markPublished();
      outboxRepository.save(row);
      log.debug("Outbox event {} published", row.getEventId());
    } catch (Exception e) {
      row.incrementAttempts();
      if (row.getAttempts() >= maxAttempts) {
        row.markFailed();
        log.error(
            "Outbox event {} failed after {} attempts — marked FAILED",
            row.getEventId(),
            row.getAttempts(),
            e);
      } else {
        log.warn(
            "Outbox publish failed for event {} (attempt {}), will retry",
            row.getEventId(),
            row.getAttempts(),
            e);
      }
      outboxRepository.save(row);
    }
  }

  /** Purge PUBLISHED rows older than the retention window (ADR-0006 cleanup job). */
  @Scheduled(fixedDelayString = "${outbox.cleanup-interval:PT24H}")
  public void cleanPublishedEvents() {
    Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
    long deleted =
        outboxRepository.deleteByStatusAndPublishedAtBefore(UserOutboxStatus.PUBLISHED, cutoff);
    if (deleted > 0) {
      log.info("Cleaned {} published outbox rows older than {} days", deleted, retentionDays);
    }
  }
}
