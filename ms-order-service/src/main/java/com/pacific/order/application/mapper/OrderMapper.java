package com.pacific.order.application.mapper;

import com.pacific.order.application.dto.OrderItemDto;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.domain.model.Order;
import com.pacific.order.domain.model.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper to convert between domain models and DTOs
 */
public class OrderMapper {

    private OrderMapper() {
        // Utility class
    }

    /**
     * Convert Order domain model to OrderResponse DTO
     */
    public static OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponse.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .items(toItemDtos(order.getItems()))
            .totalAmount(order.getTotalAmount().getAmount())
            .currency(order.getTotalAmount().getCurrency().getCurrencyCode())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .createdBy(order.getCreatedBy())
            .updatedBy(order.getUpdatedBy())
            .build();
    }

    /**
     * Convert list of OrderItem to list of OrderItemDto
     */
    public static List<OrderItemDto> toItemDtos(List<OrderItem> items) {
        if (items == null) {
            return null;
        }

        return items.stream()
            .map(OrderMapper::toItemDto)
            .collect(Collectors.toList());
    }

    /**
     * Convert OrderItem to OrderItemDto
     */
    public static OrderItemDto toItemDto(OrderItem item) {
        if (item == null) {
            return null;
        }

        return OrderItemDto.builder()
            .productName(item.getProductName())
            .description(item.getDescription())
            .quantity(item.getQuantity())
            .price(item.getUnitPrice().getAmount())
            .build();
    }
}

