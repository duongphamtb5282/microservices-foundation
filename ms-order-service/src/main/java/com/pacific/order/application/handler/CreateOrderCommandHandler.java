package com.pacific.order.application.handler;

import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
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
import com.pacific.order.infrastructure.messaging.publisher.OrderEventPublisher;
import com.pacific.core.messaging.metrics.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handler for CreateOrderCommand
 * Implements backend-core CommandHandler interface
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderCommandHandler implements CommandHandler<CreateOrderCommand, OrderResponse> {

    private final OrderRepository orderRepository;
    private final OrderDomainService orderDomainService;
    private final OrderEventPublisher eventPublisher;
    private final EventStoreRepository eventStoreRepository;
    private final BusinessMetricsService businessMetricsService;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public CommandResult<OrderResponse> handle(CreateOrderCommand command) {
        try {
            log.info("Handling CreateOrderCommand for user: {}", command.getUserId());

            // 1. Create domain order
            Order order = orderDomainService.createOrder(
                command.getUserId(),
                command.getItems(),
                command.getInitiator()
            );

            // 2. Validate domain rules
            order.validate();

            // 3. Save to database
            Order savedOrder = orderRepository.save(order);

            // 4. Create and save event sourcing event
            OrderCreatedEventV2 eventSourcingEvent = OrderCreatedEventV2.builder()
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

            // 5. Publish domain event to Kafka (for external consumers)
            OrderCreatedEvent kafkaEvent = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .items(savedOrder.getItems())
                .totalAmount(savedOrder.getTotalAmount())
                .timestamp(Instant.now())
                .correlationId(command.getCorrelationId())
                .build();

            eventPublisher.publishOrderCreated(kafkaEvent);

            // 6. Evict user orders cache since new order was added
            evictUserOrdersCache(savedOrder.getUserId());

            // 7. Record business metrics
            businessMetricsService.recordOrderCreated(
                savedOrder.getUserId(),
                savedOrder.getTotalAmount().getAmount().doubleValue()
            );
            businessMetricsService.recordUserActivity(savedOrder.getUserId());

            // 8. Return response
            OrderResponse response = OrderMapper.toResponse(savedOrder);

            log.info("Order created successfully: {}", savedOrder.getId());
            return CommandResult.success(response);

        } catch (InvalidOrderException e) {
            log.error("Invalid order: {}", e.getMessage());
            return CommandResult.failure(e.getMessage(), "INVALID_ORDER");

        } catch (Exception e) {
            log.error("Failed to create order", e);
            return CommandResult.failure(
                "Failed to create order: " + e.getMessage(),
                "ORDER_CREATION_FAILED"
            );
        }
    }

    /**
     * Evict user orders cache when order is modified.
     */
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

