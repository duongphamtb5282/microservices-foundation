package com.pacific.payment.modules.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for Payment
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    
    private String id;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    
    // Gateway details
    private String gatewayTransactionId;
    private String gatewayResponse;
    
    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Integer version;

    /**
     * Validate payment
     */
    public void validate() {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (method == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Payment status is required");
        }
    }

    /**
     * Complete the payment
     */
    public void complete(String transactionId, String response) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Payment is already in terminal state: " + status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.gatewayTransactionId = transactionId;
        this.gatewayResponse = response;
    }

    /**
     * Fail the payment
     */
    public void fail(String response) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Payment is already in terminal state: " + status);
        }
        this.status = PaymentStatus.FAILED;
        this.gatewayResponse = response;
    }
}

