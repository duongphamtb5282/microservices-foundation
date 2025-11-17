package com.pacific.payment.modules.consumer.handler;

import com.pacific.core.messaging.cqrs.command.CommandBus;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.payment.modules.consumer.event.OrderCreatedEvent;
import com.pacific.payment.modules.payment.command.CreatePaymentCommand;
import com.pacific.payment.modules.payment.domain.PaymentMethod;
import com.pacific.payment.modules.payment.dto.PaymentResponse;
import com.pacific.payment.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for OrderCreatedEvent
 * Processes the event and creates a payment
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedEventHandler {

    private final CommandBus commandBus;
    private final PaymentService paymentService;

    @Transactional
    public void handle(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent: orderId={}, userId={}, amount={}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount());

        try {
            // 1. Check if payment already exists (idempotency)
            if (paymentService.existsByOrderId(event.getOrderId())) {
                log.warn("Payment already exists for order: {}, skipping", event.getOrderId());
                return;
            }

            // 2. Create payment command
            CreatePaymentCommand command = CreatePaymentCommand.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .method(PaymentMethod.CREDIT_CARD)  // Default
                .initiator("SYSTEM")
                .correlationId(event.getCorrelationId())
                .build();

            // 3. Execute command via CommandBus
            CommandResult<PaymentResponse> result = commandBus.execute(command);

            if (!result.isSuccess()) {
                log.error("Failed to create payment for order {}: {}",
                        event.getOrderId(), result.getErrorMessage());
                throw new PaymentCreationException(
                    "Failed to create payment: " + result.getErrorMessage()
                );
            }

            log.info("Payment created successfully for order: {}, paymentId: {}",
                    event.getOrderId(), result.getData().getPaymentId());

        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent for order: {}",
                    event.getOrderId(), e);
            throw e;  // Will trigger retry via retry mechanism
        }
    }

    /**
     * Exception for payment creation failures
     */
    public static class PaymentCreationException extends RuntimeException {
        public PaymentCreationException(String message) {
            super(message);
        }
    }
}

