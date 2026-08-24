package com.pacific.auth.modules.outbox.entity;

import com.pacific.auth.modules.outbox.UserOutboxStatus;
import com.pacific.shared.events.UserCreatedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transactional outbox row (ADR-0006). Written in the same DB transaction as the user; published
 * to Kafka by {@code UserOutboxRelay}; then marked PUBLISHED.
 */
@Entity
@Table(
    name = "user_outbox",
    schema = "auth",
    indexes = @Index(name = "idx_user_outbox_status_created", columnList = "status, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class UserOutboxEntity {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @Column(name = "event_id", nullable = false, unique = true)
  private String eventId;

  @Column(name = "aggregate_id")
  private String aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserOutboxStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  /** Build a PENDING outbox row from the event and its JSON payload. */
  public static UserOutboxEntity from(UserCreatedEvent event, String payload) {
    UserOutboxEntity entity = new UserOutboxEntity();
    entity.id = UUID.randomUUID().toString();
    entity.eventId = event.getUserId();
    entity.aggregateId = event.getAggregateId();
    entity.eventType = event.getEventType();
    entity.payload = payload;
    entity.status = UserOutboxStatus.PENDING;
    entity.attempts = 0;
    entity.createdAt = Instant.now();
    return entity;
  }

  /** Transition PENDING -> PUBLISHED after the Kafka send succeeds. */
  public void markPublished() {
    this.status = UserOutboxStatus.PUBLISHED;
    this.publishedAt = Instant.now();
  }

  /** Increment the publish attempt counter. */
  public void incrementAttempts() {
    this.attempts++;
  }

  /** Transition to FAILED when the max attempt count is reached. */
  public void markFailed() {
    this.status = UserOutboxStatus.FAILED;
  }
}
