package com.pacific.order.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Domain model representing an item in an order
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    private String id;
    private String orderId;
    private String productName;
    private String description;
    private Integer quantity;
    private Money unitPrice;
    private Money totalPrice;

    /**
     * Calculate total price based on quantity and unit price
     */
    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(quantity);
        }
    }

    /**
     * Get total price for this item
     */
    public Money getTotalPrice() {
        return totalPrice;
    }

    /**
     * Validate the order item
     */
    public void validate() {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.isNegative()) {
            throw new IllegalArgumentException("Unit price must be positive or zero");
        }
    }
}

