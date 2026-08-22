/*
 * Copyright (c) 2025 Demo Company. All rights reserved.
 *
 * This file is part of the Microservices Demo project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pacific.order.infrastructure.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pacific.order.domain.event.OrderCreatedEvent;
import com.pacific.order.domain.model.Money;
import com.pacific.order.domain.model.OrderItem;
import com.pacific.order.infrastructure.messaging.publisher.OrderEventPublisher;
import com.pacific.order.infrastructure.outbox.OrderOutboxStatus;
import com.pacific.order.infrastructure.outbox.config.OrderOutboxConfig;
import com.pacific.order.infrastructure.outbox.entity.OrderOutboxEntity;
import com.pacific.order.infrastructure.outbox.repository.OrderOutboxJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for the outbox relay (ADR-0001): publish, mark PUBLISHED, retry, FAILED. */
class OrderOutboxRelayTest {

  private final OrderOutboxJpaRepository repository = mock(OrderOutboxJpaRepository.class);
  private final OrderEventPublisher publisher = mock(OrderEventPublisher.class);
  private final ObjectMapper objectMapper = new OrderOutboxConfig().orderOutboxObjectMapper();

  private OrderOutboxRelay relay;

  @BeforeEach
  void setUp() {
    relay = new OrderOutboxRelay(repository, publisher, objectMapper);
    ReflectionTestUtils.setField(relay, "batchSize", 100);
    ReflectionTestUtils.setField(relay, "maxAttempts", 5);
    ReflectionTestUtils.setField(relay, "retentionDays", 7);
  }

  @Test
  void publishesPendingEventAndMarksPublished() throws Exception {
    OrderCreatedEvent event = orderCreatedEvent();
    OrderOutboxEntity row = OrderOutboxEntity.from(event, objectMapper.writeValueAsString(event));
    when(repository.findByStatusOrderByCreatedAtAsc(OrderOutboxStatus.PENDING, any(Pageable.class)))
        .thenReturn(List.of(row));
    CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
    future.complete(mock(SendResult.class));
    when(publisher.publishOrderCreated(any(OrderCreatedEvent.class))).thenReturn(future);

    relay.publishPendingEvents();

    assertThat(row.getStatus()).isEqualTo(OrderOutboxStatus.PUBLISHED);
    assertThat(row.getPublishedAt()).isNotNull();
    verify(repository).save(row);
  }

  @Test
  void failedSendRetriesThenMarksFailed() throws Exception {
    OrderCreatedEvent event = orderCreatedEvent();
    OrderOutboxEntity row = OrderOutboxEntity.from(event, objectMapper.writeValueAsString(event));
    when(repository.findByStatusOrderByCreatedAtAsc(OrderOutboxStatus.PENDING, any(Pageable.class)))
        .thenReturn(List.of(row));
    CompletableFuture<SendResult<String, OrderCreatedEvent>> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException("kafka down"));
    when(publisher.publishOrderCreated(any(OrderCreatedEvent.class))).thenReturn(future);

    // First failure: attempts incremented, still PENDING (below max-attempts)
    relay.publishPendingEvents();
    assertThat(row.getAttempts()).isEqualTo(1);
    assertThat(row.getStatus()).isEqualTo(OrderOutboxStatus.PENDING);

    // With max-attempts=1 the same failure flips the row to FAILED
    ReflectionTestUtils.setField(relay, "maxAttempts", 1);
    relay.publishPendingEvents();
    assertThat(row.getAttempts()).isEqualTo(2);
    assertThat(row.getStatus()).isEqualTo(OrderOutboxStatus.FAILED);
  }

  private OrderCreatedEvent orderCreatedEvent() {
    return OrderCreatedEvent.builder()
        .orderId("order-1")
        .userId("user-1")
        .items(
            List.of(
                OrderItem.builder()
                    .id("i1")
                    .productName("Laptop")
                    .quantity(2)
                    .unitPrice(Money.usd(BigDecimal.TEN))
                    .build()))
        .totalAmount(Money.usd(BigDecimal.valueOf(20)))
        .correlationId("corr-1")
        .build();
  }
}
