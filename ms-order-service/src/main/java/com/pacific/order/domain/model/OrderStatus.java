package com.pacific.order.domain.model;

/**
 * Order status enum representing the lifecycle of an order
 */
public enum OrderStatus {
    PENDING("Pending", "Order has been created and awaiting processing"),
    CONFIRMED("Confirmed", "Order has been confirmed"),
    PROCESSING("Processing", "Order is being processed"),
    SHIPPED("Shipped", "Order has been shipped"),
    DELIVERED("Delivered", "Order has been delivered"),
    COMPLETED("Completed", "Order is completed"),
    CANCELLED("Cancelled", "Order has been cancelled"),
    FAILED("Failed", "Order processing failed");

    private final String displayName;
    private final String description;

    OrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (this.isTerminal()) {
            return false;
        }

        return switch (this) {
            case PENDING -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == PROCESSING || newStatus == CANCELLED;
            case PROCESSING -> newStatus == SHIPPED || newStatus == FAILED || newStatus == CANCELLED;
            case SHIPPED -> newStatus == DELIVERED || newStatus == FAILED;
            case DELIVERED -> newStatus == COMPLETED;
            default -> false;
        };
    }
}

