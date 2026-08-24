package com.pacific.auth.modules.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.auth.modules.outbox.UserOutboxStatus;
import com.pacific.auth.modules.outbox.entity.UserOutboxEntity;
import com.pacific.auth.modules.outbox.repository.UserOutboxJpaRepository;
import com.pacific.core.messaging.cqrs.event.EventPublisher;
import com.pacific.shared.events.UserCreatedEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the user outbox relay (ADR-0006): publish, mark PUBLISHED, retry, FAILED. */
class UserOutboxRelayTest {

  private final UserOutboxJpaRepository repository = mock(UserOutboxJpaRepository.class);
  private final EventPublisher publisher = mock(EventPublisher.class);
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private UserOutboxRelay relay;
  private final ExecutorService executor = Executors.newFixedThreadPool(2);

  @BeforeEach
  void setUp() {
    relay = new UserOutboxRelay(repository, publisher, objectMapper, executor);
    ReflectionTestUtils.setField(relay, "batchSize", 100);
    ReflectionTestUtils.setField(relay, "maxAttempts", 5);
    ReflectionTestUtils.setField(relay, "retentionDays", 7);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void publishesPendingEventAndMarksPublished() throws Exception {
    UserCreatedEvent event = new UserCreatedEvent("user-1", "johndoe", "john@example.com");
    UserOutboxEntity row = UserOutboxEntity.from(event, objectMapper.writeValueAsString(event));
    when(repository.findByStatusOrderByCreatedAtAsc(
            eq(UserOutboxStatus.PENDING), any(Pageable.class)))
        .thenReturn(List.of(row));
    CompletableFuture<SendResult<String, UserCreatedEvent>> future = new CompletableFuture<>();
    future.complete(mock(SendResult.class));
    when(publisher.publish(eq("user-events"), eq("user-1"), any(UserCreatedEvent.class)))
        .thenReturn(future);

    relay.publishPendingEvents();

    assertThat(row.getStatus()).isEqualTo(UserOutboxStatus.PUBLISHED);
    assertThat(row.getPublishedAt()).isNotNull();
    verify(repository).save(row);
  }

  @Test
  void failedSendRetriesThenMarksFailed() throws Exception {
    UserCreatedEvent event = new UserCreatedEvent("user-1", "johndoe", "john@example.com");
    UserOutboxEntity row = UserOutboxEntity.from(event, objectMapper.writeValueAsString(event));
    when(repository.findByStatusOrderByCreatedAtAsc(
            eq(UserOutboxStatus.PENDING), any(Pageable.class)))
        .thenReturn(List.of(row));
    CompletableFuture<SendResult<String, UserCreatedEvent>> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("kafka down"));
    when(publisher.publish(eq("user-events"), eq("user-1"), any(UserCreatedEvent.class)))
        .thenReturn(future);

    // First failure: attempts incremented, still PENDING (below max-attempts)
    relay.publishPendingEvents();
    assertThat(row.getAttempts()).isEqualTo(1);
    assertThat(row.getStatus()).isEqualTo(UserOutboxStatus.PENDING);

    // With max-attempts=1 the same failure flips the row to FAILED
    ReflectionTestUtils.setField(relay, "maxAttempts", 1);
    relay.publishPendingEvents();
    assertThat(row.getAttempts()).isEqualTo(2);
    assertThat(row.getStatus()).isEqualTo(UserOutboxStatus.FAILED);
  }
}
