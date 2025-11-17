package com.pacific.order.application.handler;

import com.pacific.core.messaging.cqrs.query.QueryHandler;
import com.pacific.core.messaging.cqrs.query.QueryResult;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.application.mapper.OrderMapper;
import com.pacific.order.application.query.GetOrderByIdQuery;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Handler for GetOrderByIdQuery
 * Implements backend-core QueryHandler interface with caching support
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrderByIdQueryHandler implements QueryHandler<GetOrderByIdQuery, OrderResponse> {

    private final OrderRepository orderRepository;

    @Override
    @Cacheable(value = "order-details", key = "#query.orderId")
    public QueryResult<OrderResponse> handle(GetOrderByIdQuery query) {
        try {
            log.debug("Handling GetOrderByIdQuery for order: {}", query.getOrderId());

            Optional<Order> orderOpt = orderRepository.findById(query.getOrderId());

            if (orderOpt.isEmpty()) {
                log.debug("Order not found: {}", query.getOrderId());
                return QueryResult.empty();
            }

            Order order = orderOpt.get();
            OrderResponse response = OrderMapper.toResponse(order);

            log.debug("Order found: {} (cached)", query.getOrderId());
            return QueryResult.of(response);

        } catch (Exception e) {
            log.error("Failed to get order by ID", e);
            return QueryResult.of(null);
        }
    }
}

