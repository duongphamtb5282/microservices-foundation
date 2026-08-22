package com.pacific.order.application.handler;

import com.pacific.order.domain.event.OrderStatusUpdatedEvent;
import com.pacific.order.domain.exception.OrderNotFoundException;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.model.OrderStatus;
import com.pacific.order.domain.repository.OrderRepository;
import com.pacific.order.infrastructure.eventsourcing.EventStoreRepository;
import com.pacific.order.infrastructure.messaging.event.PaymentResultEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga return path (F-02): settles an order from the payment result event. Payment COMPLETED ->
 * order CONFIRMED; payment FAILED -> order FAILED. Idempotent: already-settled orders are skipped,
 * so at-least-once delivery from the payment outbox is safe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultEventHandler {

  private final OrderRepository orderRepository;
  private final EventStoreRepository eventStoreRepository;
  private final CacheManager cacheManager;

  @Transactional(rollbackFor = Exception.class)
  public void handle(PaymentResultEvent event) {
    log.info(
        "Settling order from payment result: orderId={}, paymentStatus={}",
        event.getOrderId(),
        event.getStatus());

    Order order =
        orderRepository
            .findById(event.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

    // Idempotent by construction: terminal or already-CONFIRMED orders ignore repeat events
    // (the payment outbox is at-least-once; a replayed event must not rewrite settled state)
    if (order.getStatus().isTerminal() || order.getStatus() == OrderStatus.CONFIRMED) {
      log.debug(
          "Order {} already settled ({}), ignoring payment result: {}",
          order.getId(),
          order.getStatus(),
          event.getStatus());
      return;
    }

    OrderStatus target =
        "COMPLETED".equals(event.getStatus()) ? OrderStatus.CONFIRMED : OrderStatus.FAILED;

    if (!order.getStatus().canTransitionTo(target)) {
      // Stale/out-of-order event — the order moved on; nothing to do (do not fail the consumer)
      log.warn(
          "Cannot settle order {} from {} to {} — skipping stale payment result",
          order.getId(),
          order.getStatus(),
          target);
      return;
    }

    OrderStatus oldStatus = order.getStatus();
    order.updateStatus(target);
    order.setUpdatedAt(LocalDateTime.now());
    order.setUpdatedBy("payment-service");

    Order savedOrder = orderRepository.save(order);

    eventStoreRepository.saveEvent(
        OrderStatusUpdatedEvent.builder()
            .orderId(savedOrder.getId())
            .userId(savedOrder.getUserId())
            .oldStatus(oldStatus)
            .newStatus(target)
            .eventTimestamp(Instant.now())
            .correlationId(event.getCorrelationId())
            .updatedBy("payment-service")
            .version(savedOrder.getVersion() + 1)
            .build());

    evictOrderCaches(savedOrder.getId(), savedOrder.getUserId());

    log.info(
        "Order {} settled: {} -> {} (paymentId: {})",
        savedOrder.getId(),
        oldStatus,
        target,
        event.getPaymentId());
  }

  /** Evict all caches related to the order and user. */
  private void evictOrderCaches(String orderId, String userId) {
    try {
      var orderDetailsCache = cacheManager.getCache("order-details");
      if (orderDetailsCache != null) {
        orderDetailsCache.evict(orderId);
      }
      var userOrdersCache = cacheManager.getCache("user-orders");
      if (userOrdersCache != null) {
        userOrdersCache.evict(userId);
      }
    } catch (Exception e) {
      log.warn("Failed to evict caches for order: {} and user: {}", orderId, userId, e);
      // Don't fail the settlement if cache eviction fails
    }
  }
}
