package com.pacific.order.application.handler;

import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.metrics.BusinessMetricsService;
import com.pacific.order.application.command.CreateOrderCommand;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.application.mapper.OrderMapper;
import com.pacific.order.domain.event.OrderCreatedEvent;
import com.pacific.order.domain.event.OrderCreatedEventV2;
import com.pacific.order.domain.exception.InvalidOrderException;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.repository.OrderRepository;
import com.pacific.order.domain.service.OrderDomainService;
import com.pacific.order.infrastructure.eventsourcing.EventStoreRepository;
import com.pacific.order.infrastructure.outbox.service.OrderOutboxService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Handler for CreateOrderCommand Implements backend-core CommandHandler interface */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderCommandHandler
    implements CommandHandler<CreateOrderCommand, OrderResponse> {

  private final OrderRepository orderRepository;
  private final OrderDomainService orderDomainService;
  private final OrderOutboxService orderOutboxService;
  private final EventStoreRepository eventStoreRepository;
  private final BusinessMetricsService businessMetricsService;
  private final CacheManager cacheManager;

  @Override
  @Transactional
  public CommandResult<OrderResponse> handle(CreateOrderCommand command) {
    try {
      log.info("Handling CreateOrderCommand for user: {}", command.getUserId());

      // 1. Create domain order
      Order order =
          orderDomainService.createOrder(
              command.getUserId(), command.getItems(), command.getInitiator());

      // 2. Validate domain rules
      order.validate();

      // 3. Save to database
      Order savedOrder = orderRepository.save(order);

      // 4. Create and save event sourcing event
      OrderCreatedEventV2 eventSourcingEvent =
          OrderCreatedEventV2.builder()
              .orderId(savedOrder.getId())
              .userId(savedOrder.getUserId())
              .items(savedOrder.getItems())
              .totalAmount(savedOrder.getTotalAmount())
              .eventTimestamp(Instant.now())
              .correlationId(command.getCorrelationId())
              .createdBy(command.getInitiator())
              .version(1)
              .build();

      eventStoreRepository.saveEvent(eventSourcingEvent);

      // 5. Write to the transactional outbox — OrderOutboxRelay publishes to Kafka (ADR-0001).
      //    The outbox row commits atomically with the order; no Kafka call inside the tx.
      OrderCreatedEvent kafkaEvent =
          OrderCreatedEvent.builder()
              .orderId(savedOrder.getId())
              .userId(savedOrder.getUserId())
              .items(savedOrder.getItems())
              .totalAmount(savedOrder.getTotalAmount())
              .timestamp(Instant.now())
              .correlationId(command.getCorrelationId())
              .build();

      orderOutboxService.record(kafkaEvent);

      // 6. Evict user orders cache since new order was added
      evictUserOrdersCache(savedOrder.getUserId());

      // 7. Record business metrics
      businessMetricsService.recordOrderCreated(
          savedOrder.getUserId(), savedOrder.getTotalAmount().getAmount().doubleValue());
      businessMetricsService.recordUserActivity(savedOrder.getUserId());

      // 8. Return response
      OrderResponse response = OrderMapper.toResponse(savedOrder);

      log.info("Order created successfully: {}", savedOrder.getId());
      return CommandResult.success(response);

    } catch (InvalidOrderException e) {
      // Business validation failure — thrown before any persistence, nothing to roll back
      log.error("Invalid order: {}", e.getMessage());
      return CommandResult.failure(e.getMessage(), "INVALID_ORDER");

    } catch (RuntimeException e) {
      // F-25: propagate so @Transactional rolls back. Returning failure here would let a partial
      // write (order + outbox row) commit while the client sees an error — retry then duplicates.
      log.error("Failed to create order — transaction will roll back", e);
      throw e;
    }
  }

  /** Evict user orders cache when order is modified. */
  private void evictUserOrdersCache(String userId) {
    try {
      // Evict user orders cache
      var userOrdersCache = cacheManager.getCache("user-orders");
      if (userOrdersCache != null) {
        userOrdersCache.evict(userId);
        log.debug("Evicted user orders cache for user: {}", userId);
      }
    } catch (Exception e) {
      log.warn("Failed to evict user orders cache for user: {}", userId, e);
      // Don't fail the operation if cache eviction fails
    }
  }
}
