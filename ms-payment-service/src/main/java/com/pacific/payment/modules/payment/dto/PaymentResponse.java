package com.pacific.payment.modules.payment.dto;

import com.pacific.payment.modules.payment.domain.PaymentMethod;
import com.pacific.payment.modules.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    
    private String paymentId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

