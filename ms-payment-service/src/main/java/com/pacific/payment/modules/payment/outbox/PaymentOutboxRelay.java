package com.pacific.payment.modules.payment.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.payment.modules.payment.event.PaymentResultEvent;
import com.pacific.payment.modules.payment.outbox.entity.PaymentOutboxEntity;
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
 * Read side of the payment result outbox (F-02). Polls PENDING rows and publishes them to Kafka,
 * marking each row PUBLISHED only after the send succeeds; rows that exhaust {@code
 * payment.outbox.max-attempts} become FAILED for manual inspection. At-least-once delivery is safe
 * because the order service skips already-settled orders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentOutboxRelay {

  private final PaymentOutboxJpaRepository outboxRepository;
  private final PaymentEventPublisher eventPublisher;
  private final ObjectMapper paymentOutboxObjectMapper;
  @Qualifier("outboxPublisherExecutor")
  private final Executor outboxPublisherExecutor;

  @Value("${payment.outbox.batch-size:100}")
  private int batchSize;

  @Value("${payment.outbox.max-attempts:5}")
  private int maxAttempts;

  @Value("${payment.outbox.retention-days:7}")
  private long retentionDays;

  @Scheduled(fixedDelayString = "${payment.outbox.poll-interval:PT0.5S}")
  public void publishPendingEvents() {
    List<PaymentOutboxEntity> pending =
        outboxRepository.findByStatusOrderByCreatedAtAsc(
            PaymentOutboxStatus.PENDING, PageRequest.of(0, batchSize));

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

  private void publishRow(PaymentOutboxEntity row) {
    try {
      PaymentResultEvent event =
          paymentOutboxObjectMapper.readValue(row.getPayload(), PaymentResultEvent.class);
      eventPublisher.publishPaymentResult(event).get(10, TimeUnit.SECONDS);
      row.markPublished();
      outboxRepository.save(row);
      log.debug("Payment outbox event {} published", row.getEventId());
    } catch (Exception e) {
      row.incrementAttempts();
      if (row.getAttempts() >= maxAttempts) {
        row.markFailed();
        log.error(
            "Payment outbox event {} failed after {} attempts — marked FAILED",
            row.getEventId(),
            row.getAttempts(),
            e);
      } else {
        log.warn(
            "Payment outbox publish failed for event {} (attempt {}), will retry",
            row.getEventId(),
            row.getAttempts(),
            e);
      }
      outboxRepository.save(row);
    }
  }

  /** Purge PUBLISHED rows older than the retention window. */
  @Scheduled(fixedDelayString = "${payment.outbox.cleanup-interval:PT24H}")
  public void cleanPublishedEvents() {
    Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
    long deleted =
        outboxRepository.deleteByStatusAndPublishedAtBefore(PaymentOutboxStatus.PUBLISHED, cutoff);
    if (deleted > 0) {
      log.info(
          "Cleaned {} published payment outbox rows older than {} days", deleted, retentionDays);
    }
  }
}
