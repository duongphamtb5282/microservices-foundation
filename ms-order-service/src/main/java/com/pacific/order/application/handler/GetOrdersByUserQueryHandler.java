package com.pacific.order.application.handler;

import com.pacific.core.messaging.cqrs.query.QueryHandler;
import com.pacific.core.messaging.cqrs.query.QueryResult;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.application.mapper.OrderMapper;
import com.pacific.order.application.query.GetOrdersByUserQuery;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for GetOrdersByUserQuery with caching support
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrdersByUserQueryHandler implements QueryHandler<GetOrdersByUserQuery, List<OrderResponse>> {

    private final OrderRepository orderRepository;

    @Override
    @Cacheable(value = "user-orders", key = "#query.userId")
    public QueryResult<List<OrderResponse>> handle(GetOrdersByUserQuery query) {
        try {
            log.debug("Handling GetOrdersByUserQuery for user: {}", query.getUserId());

            List<Order> orders = orderRepository.findByUserId(query.getUserId());

            List<OrderResponse> responses = orders.stream()
                .map(OrderMapper::toResponse)
                .collect(Collectors.toList());

            log.debug("Found {} orders for user: {} (cached)", responses.size(), query.getUserId());
            return QueryResult.of(responses);

        } catch (Exception e) {
            log.error("Failed to get orders by user", e);
            return QueryResult.of(null);
        }
    }
}

