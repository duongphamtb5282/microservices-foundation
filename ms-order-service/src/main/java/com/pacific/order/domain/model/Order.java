package com.pacific.order.domain.model;

import com.pacific.order.domain.exception.InvalidOrderException;
import com.pacific.order.domain.exception.OrderCannotBeCancelledException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain model representing an Order (Aggregate Root)
 * Contains core business logic and invariants
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private String id;
    private String userId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;

    // Customer information (potentially sensitive)
    private String customerEmail;  // Will be encrypted in persistence
    private String customerPhone;  // Will be encrypted in persistence
    private String shippingAddress; // Will be encrypted in persistence

    // Security fields
    private String dataEncryptionKeyId; // For key rotation support

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Integer version;

    /**
     * Validate order domain rules
     */
    public void validate() {
        if (userId == null || userId.isBlank()) {
            throw new InvalidOrderException("User ID is required");
        }

        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must have at least one item");
        }

        // Validate each item
        items.forEach(OrderItem::validate);

        if (totalAmount == null || totalAmount.isNegative()) {
            throw new InvalidOrderException("Total amount must be positive or zero");
        }

        if (status == null) {
            throw new InvalidOrderException("Order status is required");
        }
    }

    /**
     * Calculate total amount from items
     */
    public void calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            this.totalAmount = Money.zero("USD");
            return;
        }

        Money total = Money.zero("USD");
        for (OrderItem item : items) {
            item.calculateTotalPrice();
            total = total.add(item.getTotalPrice());
        }
        this.totalAmount = total;
    }

    /**
     * Cancel the order (business logic)
     */
    public void cancel() {
        if (status == OrderStatus.COMPLETED) {
            throw new OrderCannotBeCancelledException("Cannot cancel a completed order");
        }
        if (status == OrderStatus.DELIVERED) {
            throw new OrderCannotBeCancelledException("Cannot cancel a delivered order");
        }
        if (status.isTerminal()) {
            throw new OrderCannotBeCancelledException("Cannot cancel an order in terminal state: " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * Confirm the order
     */
    public void confirm() {
        if (!status.canTransitionTo(OrderStatus.CONFIRMED)) {
            throw new InvalidOrderException("Cannot confirm order in current status: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * Update order status
     */
    public void updateStatus(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidOrderException(
                String.format("Cannot transition from %s to %s", status, newStatus)
            );
        }
        this.status = newStatus;
    }
}

