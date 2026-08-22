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
package com.pacific.order.application.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.metrics.BusinessMetricsService;
import com.pacific.order.application.command.CreateOrderCommand;
import com.pacific.order.application.dto.OrderItemDto;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.domain.event.OrderCreatedEvent;
import com.pacific.order.domain.event.OrderCreatedEventV2;
import com.pacific.order.domain.model.Money;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.model.OrderItem;
import com.pacific.order.domain.model.OrderStatus;
import com.pacific.order.domain.repository.OrderRepository;
import com.pacific.order.domain.service.OrderDomainService;
import com.pacific.order.infrastructure.eventsourcing.EventStoreRepository;
import com.pacific.order.infrastructure.outbox.service.OrderOutboxService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * Regression tests for ADR-0001 (outbox write instead of direct Kafka publish) and F-25 (no partial
 * commits on failure).
 */
class CreateOrderCommandHandlerTest {

  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final OrderDomainService orderDomainService = mock(OrderDomainService.class);
  private final OrderOutboxService orderOutboxService = mock(OrderOutboxService.class);
  private final EventStoreRepository eventStoreRepository = mock(EventStoreRepository.class);
  private final BusinessMetricsService businessMetricsService = mock(BusinessMetricsService.class);
  private final CacheManager cacheManager = mock(CacheManager.class);

  private final CreateOrderCommandHandler handler =
      new CreateOrderCommandHandler(
          orderRepository,
          orderDomainService,
          orderOutboxService,
          eventStoreRepository,
          businessMetricsService,
          cacheManager);

  @Test
  void successfulOrderWritesOutboxAndDoesNotPublishDirectly() {
    Order order = validOrder();
    when(orderDomainService.createOrder(any(), any(), any())).thenReturn(order);
    when(orderRepository.save(order)).thenReturn(order);

    CommandResult<OrderResponse> result = handler.handle(command());

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getData().getOrderId()).isEqualTo("order-1");

    // ADR-0001: the event goes to the outbox, never straight to Kafka from inside the tx
    ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
    verify(orderOutboxService).record(captor.capture());
    assertThat(captor.getValue().getOrderId()).isEqualTo("order-1");
    verify(orderRepository).save(order);
  }

  @Test
  void persistenceFailurePropagatesSoTransactionRollsBack() {
    Order order = validOrder();
    when(orderDomainService.createOrder(any(), any(), any())).thenReturn(order);
    when(orderRepository.save(order)).thenReturn(order);
    doThrow(new RuntimeException("db down"))
        .when(eventStoreRepository)
        .saveEvent(any(OrderCreatedEventV2.class));

    // F-25: the exception must escape the @Transactional boundary (rollback), not be converted
    // into CommandResult.failure while the commit still happens
    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(RuntimeException.class);

    verify(orderOutboxService, never()).record(any());
  }

  private CreateOrderCommand command() {
    return CreateOrderCommand.builder()
        .userId("user-1")
        .items(
            List.of(
                OrderItemDto.builder()
                    .productName("Laptop")
                    .quantity(2)
                    .price(BigDecimal.TEN)
                    .build()))
        .initiator("tester")
        .correlationId("corr-1")
        .build();
  }

  private Order validOrder() {
    return Order.builder()
        .id("order-1")
        .userId("user-1")
        .items(
            List.of(
                OrderItem.builder()
                    .id("item-1")
                    .productName("Laptop")
                    .quantity(2)
                    .unitPrice(Money.usd(BigDecimal.TEN))
                    .build()))
        .totalAmount(Money.usd(BigDecimal.valueOf(20)))
        .status(OrderStatus.PENDING)
        .createdBy("tester")
        .build();
  }
}
