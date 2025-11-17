package com.pacific.order.application.command;

import com.pacific.core.messaging.cqrs.command.Command;
import com.pacific.order.application.dto.OrderResponse;
import lombok.Builder;
import lombok.Value;

/**
 * Command to cancel an order
 */
@Value
@Builder
public class CancelOrderCommand implements Command<OrderResponse> {

    String orderId;
    String userId;
    String reason;
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
        return "CANCEL_ORDER";
    }

    @Override
    public void validate() {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}

