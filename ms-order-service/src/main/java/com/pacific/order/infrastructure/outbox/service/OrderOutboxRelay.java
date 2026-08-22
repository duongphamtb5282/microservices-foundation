package com.pacific.order.infrastructure.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.order.domain.event.OrderCreatedEvent;
import com.pacific.order.infrastructure.messaging.publisher.OrderEventPublisher;
import com.pacific.order.infrastructure.outbox.OrderOutboxStatus;
import com.pacific.order.infrastructure.outbox.entity.OrderOutboxEntity;
import com.pacific.order.infrastructure.outbox.repository.OrderOutboxJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Read side of the transactional outbox (ADR-0001). Polls PENDING rows and publishes them to Kafka,
 * marking each row PUBLISHED only after the send succeeds; rows that exhaust {@code
 * outbox.max-attempts} become FAILED for manual inspection. At-least-once delivery is safe because
 * consumers deduplicate on orderId.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxRelay {

  private final OrderOutboxJpaRepository outboxRepository;
  private final OrderEventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  @Value("${outbox.batch-size:100}")
  private int batchSize;

  @Value("${outbox.max-attempts:5}")
  private int maxAttempts;

  @Value("${outbox.retention-days:7}")
  private long retentionDays;

  @Scheduled(fixedDelayString = "${outbox.poll-interval:PT0.5S}")
  public void publishPendingEvents() {
    List<OrderOutboxEntity> pending =
        outboxRepository.findByStatusOrderByCreatedAtAsc(
            OrderOutboxStatus.PENDING, PageRequest.of(0, batchSize));

    for (OrderOutboxEntity row : pending) {
      try {
        OrderCreatedEvent event = objectMapper.readValue(row.getPayload(), OrderCreatedEvent.class);
        eventPublisher.publishOrderCreated(event).get(10, TimeUnit.SECONDS);
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
  }

  /** Purge PUBLISHED rows older than the retention window (ADR-0001 cleanup job). */
  @Scheduled(fixedDelayString = "${outbox.cleanup-interval:PT24H}")
  public void cleanPublishedEvents() {
    Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
    long deleted =
        outboxRepository.deleteByStatusAndPublishedAtBefore(OrderOutboxStatus.PUBLISHED, cutoff);
    if (deleted > 0) {
      log.info("Cleaned {} published outbox rows older than {} days", deleted, retentionDays);
    }
  }
}
