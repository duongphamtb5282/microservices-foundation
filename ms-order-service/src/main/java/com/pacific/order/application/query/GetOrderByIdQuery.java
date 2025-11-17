package com.pacific.order.application.query;

import com.pacific.core.messaging.cqrs.query.Query;
import com.pacific.order.application.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query to get order by ID
 * Implements backend-core Query interface
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetOrderByIdQuery implements Query<OrderResponse> {

    private String orderId;
    private String correlationId;

    @Override
    public String getQueryType() {
        return "GET_ORDER_BY_ID";
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    public void validate() {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
    }
}

