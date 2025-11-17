package com.pacific.order.domain.service.impl;

import com.pacific.order.application.dto.OrderItemDto;
import com.pacific.order.domain.exception.InvalidOrderException;
import com.pacific.order.domain.model.Money;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.model.OrderItem;
import com.pacific.order.domain.model.OrderStatus;
import com.pacific.order.domain.service.OrderDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of order domain service
 * Contains core business logic
 */
@Service
@Slf4j
public class OrderDomainServiceImpl implements OrderDomainService {

    @Override
    public Order createOrder(String userId, List<OrderItemDto> itemDtos, String initiator) {
        log.debug("Creating order for user: {}", userId);

        // Convert DTOs to domain objects
        List<OrderItem> items = convertToOrderItems(itemDtos);

        // Create order
        Order order = Order.builder()
            .id(UUID.randomUUID().toString())
            .userId(userId)
            .items(items)
            .status(OrderStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .createdBy(initiator)
            .updatedBy(initiator)
            .version(0)
            .build();

        // Set order ID for all items
        items.forEach(item -> item.setOrderId(order.getId()));

        // Calculate total amount
        order.calculateTotalAmount();

        // Validate
        validateOrder(order);

        log.debug("Order created successfully: {}", order.getId());
        return order;
    }

    @Override
    public void validateOrder(Order order) {
        try {
            order.validate();
        } catch (IllegalArgumentException e) {
            throw new InvalidOrderException("Order validation failed: " + e.getMessage(), e);
        }
    }

    private List<OrderItem> convertToOrderItems(List<OrderItemDto> itemDtos) {
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemDto dto : itemDtos) {
            OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID().toString())
                .productName(dto.getProductName())
                .description(dto.getDescription())
                .quantity(dto.getQuantity())
                .unitPrice(Money.usd(dto.getPrice()))
                .build();

            item.calculateTotalPrice();
            items.add(item);
        }

        return items;
    }
}

