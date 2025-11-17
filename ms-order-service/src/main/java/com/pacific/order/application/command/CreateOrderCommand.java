package com.pacific.order.application.command;

import com.pacific.core.messaging.cqrs.command.Command;
import com.pacific.order.application.dto.OrderItemDto;
import com.pacific.order.application.dto.OrderResponse;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Command to create a new order
 * Implements backend-core Command interface
 */
@Value
@Builder
public class CreateOrderCommand implements Command<OrderResponse> {

    String userId;
    List<OrderItemDto> items;
    String initiator;
    String correlationId;

    public String getInitiator() {
        return initiator;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String getCommandType() {
        return "CREATE_ORDER";
    }

    @Override
    public void validate() {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        // Validate each item
        items.forEach(item -> {
            if (item.getProductName() == null || item.getProductName().isBlank()) {
                throw new IllegalArgumentException("Product name is required");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (item.getPrice() == null || item.getPrice().signum() <= 0) {
                throw new IllegalArgumentException("Price must be positive");
            }
        });
    }
}

