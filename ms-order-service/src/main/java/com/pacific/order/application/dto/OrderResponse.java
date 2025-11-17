package com.pacific.order.application.dto;

import com.pacific.order.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for order data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    
    private String orderId;
    private String userId;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
    private String currency;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}

