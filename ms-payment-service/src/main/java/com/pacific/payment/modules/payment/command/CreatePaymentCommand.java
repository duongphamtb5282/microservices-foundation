package com.pacific.payment.modules.payment.command;

import com.pacific.core.messaging.cqrs.command.Command;
import com.pacific.payment.modules.payment.domain.PaymentMethod;
import com.pacific.payment.modules.payment.dto.PaymentResponse;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Command to create a payment
 * Implements backend-core Command interface
 */
@Value
@Builder
public class CreatePaymentCommand implements Command<PaymentResponse> {

    String orderId;
    String userId;
    BigDecimal amount;
    PaymentMethod method;
    String initiator;
    String correlationId;

    @Override
    public String getCommandType() {
        return "CREATE_PAYMENT";
    }

    @Override
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
    }
}

