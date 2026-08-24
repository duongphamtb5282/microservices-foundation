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
import com.pacific.order.infrastructure.exception.IdempotencyReplayException;
import com.pacific.order.infrastructure.idempotency.entity.OrderIdempotencyEntity;
import com.pacific.order.infrastructure.idempotency.repository.OrderIdempotencyJpaRepository;
import com.pacific.order.infrastructure.outbox.service.OrderOutboxService;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
  // multiLevelCacheManager is @Primary; the qualifier pins this site explicitly (three CacheManager
  // beans coexist here: redis, local, composite — see config/OrderCacheConfig).
  @Qualifier("multiLevelCacheManager")
  private final CacheManager cacheManager;
  private final OrderIdempotencyJpaRepository idempotencyRepository;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CommandResult<OrderResponse> handle(CreateOrderCommand command) {
    try {
      log.info("Handling CreateOrderCommand for user: {}", command.getUserId());

      // 0. Idempotency pre-check (idempotency-proposal.md): same user + key => replay.
      //    Runs inside this tx; the (user_id, key) PK backstops concurrent duplicates — a losing
      //    insert raises DataIntegrityViolationException, which propagates and rolls back this
      //    attempt; the client's retry then finds the winner's row here and replays it.
      String idempotencyKey = command.getIdempotencyKey();
      if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        Optional<OrderIdempotencyEntity> existing =
            idempotencyRepository.findByUserIdAndIdempotencyKey(
                command.getUserId(), idempotencyKey);
        if (existing.isPresent()) {
          Order replayedOrder =
              orderRepository
                  .findById(existing.get().getOrderId())
                  .orElseThrow(
                      () ->
                          new IdempotencyReplayException(
                              "Idempotency key maps to missing order "
                                  + existing.get().getOrderId()));
          log.info(
              "Replayed idempotent create-order request (key={}) -> order {}",
              idempotencyKey,
              replayedOrder.getId());
          return CommandResult.success(OrderMapper.toResponse(replayedOrder));
        }
      }

      // 1. Create domain order
      Order order =
          orderDomainService.createOrder(
              command.getUserId(), command.getItems(), command.getInitiator());

      // 2. Validate domain rules
      order.validate();

      // 3. Save to database
      Order savedOrder = orderRepository.save(order);

      // 3b. Record the idempotency key in the same tx as the order + outbox row
      //     (idempotency-proposal.md): atomic — order exists <=> key exists.
      if (idempotencyKey != null && !idempotencyKey.isBlank()) {
        idempotencyRepository.save(
            OrderIdempotencyEntity.of(idempotencyKey, savedOrder.getUserId(), savedOrder.getId()));
      }

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
