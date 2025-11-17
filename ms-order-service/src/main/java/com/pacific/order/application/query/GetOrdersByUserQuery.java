package com.pacific.order.application.query;

import com.pacific.core.messaging.cqrs.query.Query;
import com.pacific.order.application.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Query to get all orders for a user
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetOrdersByUserQuery implements Query<List<OrderResponse>> {

    private String userId;
    private String correlationId;

    @Override
    public String getQueryType() {
        return "GET_ORDERS_BY_USER";
    }

    @Override
    public String getCorrelationId() {
        return correlationId;
    }

    public void validate() {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
}

