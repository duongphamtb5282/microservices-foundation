package com.pacific.order.application.handler;

import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.order.application.command.CancelOrderCommand;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.application.mapper.OrderMapper;
import com.pacific.order.domain.event.OrderCancelledEvent;
import com.pacific.order.domain.exception.OrderCannotBeCancelledException;
import com.pacific.order.domain.exception.OrderNotFoundException;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.model.OrderStatus;
import com.pacific.order.domain.repository.OrderRepository;
import com.pacific.order.infrastructure.eventsourcing.EventStoreRepository;
import com.pacific.core.messaging.metrics.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handler for CancelOrderCommand
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CancelOrderCommandHandler implements CommandHandler<CancelOrderCommand, OrderResponse> {

    private final OrderRepository orderRepository;
    private final EventStoreRepository eventStoreRepository;
    private final BusinessMetricsService businessMetricsService;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public CommandResult<OrderResponse> handle(CancelOrderCommand command) {
        try {
            log.info("Handling CancelOrderCommand for order: {}", command.getOrderId());

            // 1. Find order
            Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(command.getOrderId()));

            // 2. Verify ownership
            if (!order.getUserId().equals(command.getUserId())) {
                return CommandResult.failure(
                    "Order does not belong to user",
                    "ORDER_ACCESS_DENIED"
                );
            }

            // 3. Cancel order (domain logic)
            order.cancel();
            order.setUpdatedAt(LocalDateTime.now());
            order.setUpdatedBy(command.getInitiator());

            // 4. Save
            Order savedOrder = orderRepository.save(order);

            // 5. Create and save event sourcing event
            OrderCancelledEvent eventSourcingEvent = OrderCancelledEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .reason(command.getReason())
                .eventTimestamp(java.time.Instant.now())
                .correlationId(command.getCorrelationId())
                .cancelledBy(command.getInitiator())
                .version(savedOrder.getVersion() + 1)
                .build();

            eventStoreRepository.saveEvent(eventSourcingEvent);

            // 6. Evict caches since order was modified
            evictOrderCaches(savedOrder.getId(), savedOrder.getUserId());

            // 7. Record business metrics
            businessMetricsService.recordOrderCancelled(
                savedOrder.getUserId(),
                command.getReason()
            );

            // 8. Return response
            OrderResponse response = OrderMapper.toResponse(savedOrder);

            log.info("Order cancelled successfully: {}", savedOrder.getId());
            return CommandResult.success(response);

        } catch (OrderNotFoundException e) {
            log.error("Order not found: {}", command.getOrderId());
            return CommandResult.failure(e.getMessage(), "ORDER_NOT_FOUND");

        } catch (OrderCannotBeCancelledException e) {
            log.error("Order cannot be cancelled: {}", e.getMessage());
            return CommandResult.failure(e.getMessage(), "ORDER_CANNOT_BE_CANCELLED");

        } catch (Exception e) {
            log.error("Failed to cancel order", e);
            return CommandResult.failure(
                "Failed to cancel order: " + e.getMessage(),
                "ORDER_CANCELLATION_FAILED"
            );
        }
    }

    /**
     * Evict all caches related to the order and user.
     */
    private void evictOrderCaches(String orderId, String userId) {
        try {
            // Evict order details cache
            var orderDetailsCache = cacheManager.getCache("order-details");
            if (orderDetailsCache != null) {
                orderDetailsCache.evict(orderId);
                log.debug("Evicted order details cache for order: {}", orderId);
            }

            // Evict user orders cache
            var userOrdersCache = cacheManager.getCache("user-orders");
            if (userOrdersCache != null) {
                userOrdersCache.evict(userId);
                log.debug("Evicted user orders cache for user: {}", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict caches for order: {} and user: {}", orderId, userId, e);
            // Don't fail the operation if cache eviction fails
        }
    }
}

