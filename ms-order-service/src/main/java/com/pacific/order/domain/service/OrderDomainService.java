package com.pacific.order.domain.service;

import com.pacific.order.application.dto.OrderItemDto;
import com.pacific.order.domain.model.Order;

import java.util.List;

/**
 * Domain service interface for order-related business logic
 */
public interface OrderDomainService {
    
    /**
     * Create a new order with business logic
     */
    Order createOrder(String userId, List<OrderItemDto> items, String initiator);

    /**
     * Validate order business rules
     */
    void validateOrder(Order order);
}

