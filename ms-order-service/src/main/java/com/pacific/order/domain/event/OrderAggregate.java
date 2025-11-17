package com.pacific.order.domain.event;

import com.pacific.order.domain.model.Money;
import com.pacific.order.domain.model.OrderItem;
import com.pacific.order.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Event-sourced Order aggregate for rebuilding state from events.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderAggregate {
    private String id;
    private String userId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Integer version;

    /**
     * Apply OrderCreatedEvent to aggregate.
     */
    public void applyOrderCreated(OrderCreatedEventV2 event) {
        this.id = event.getOrderId();
        this.userId = event.getUserId();
        this.items = event.getItems();
        this.totalAmount = event.getTotalAmount();
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.createdBy = event.getCreatedBy();
        this.updatedBy = event.getCreatedBy();
        this.version = event.getVersion();
    }

    /**
     * Apply OrderCancelledEvent to aggregate.
     */
    public void applyOrderCancelled(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = event.getCancelledBy();
        this.version = event.getVersion();
    }

    /**
     * Apply OrderStatusUpdatedEvent to aggregate.
     */
    public void applyOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        this.status = event.getNewStatus();
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = event.getUpdatedBy();
        this.version = event.getVersion();
    }
}
