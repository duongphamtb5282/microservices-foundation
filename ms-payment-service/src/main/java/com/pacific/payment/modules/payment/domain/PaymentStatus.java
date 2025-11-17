package com.pacific.payment.modules.payment.domain;

/**
 * Payment status enum
 */
public enum PaymentStatus {
    PENDING("Pending", "Payment is pending"),
    PROCESSING("Processing", "Payment is being processed"),
    COMPLETED("Completed", "Payment completed successfully"),
    FAILED("Failed", "Payment failed"),
    REFUNDED("Refunded", "Payment has been refunded"),
    CANCELLED("Cancelled", "Payment was cancelled");

    private final String displayName;
    private final String description;

    PaymentStatus(String displayName, String description) {
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
        return this == COMPLETED || this == FAILED || this == REFUNDED || this == CANCELLED;
    }
}

