package com.pacific.payment.modules.payment.mapper;

import com.pacific.payment.modules.payment.domain.Payment;
import com.pacific.payment.modules.payment.dto.PaymentResponse;

/**
 * Mapper to convert between domain models and DTOs
 */
public class PaymentMapper {

    private PaymentMapper() {
        // Utility class
    }

    /**
     * Convert Payment domain model to PaymentResponse DTO
     */
    public static PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
            .paymentId(payment.getId())
            .orderId(payment.getOrderId())
            .userId(payment.getUserId())
            .amount(payment.getAmount())
            .method(payment.getMethod())
            .status(payment.getStatus())
            .gatewayTransactionId(payment.getGatewayTransactionId())
            .createdAt(payment.getCreatedAt())
            .updatedAt(payment.getUpdatedAt())
            .build();
    }
}

