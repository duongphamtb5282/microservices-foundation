package com.pacific.payment.modules.payment.handler;

import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.payment.modules.payment.command.CreatePaymentCommand;
import com.pacific.payment.modules.payment.domain.Payment;
import com.pacific.payment.modules.payment.domain.PaymentStatus;
import com.pacific.payment.modules.payment.dto.PaymentResponse;
import com.pacific.payment.modules.payment.mapper.PaymentMapper;
import com.pacific.payment.modules.payment.repository.PaymentRepository;
import com.pacific.payment.modules.payment.service.PaymentService;
import com.pacific.core.messaging.metrics.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handler for CreatePaymentCommand
 * Implements backend-core CommandHandler interface
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentCommandHandler implements CommandHandler<CreatePaymentCommand, PaymentResponse> {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final BusinessMetricsService businessMetricsService;

    @Override
    @Transactional
    public CommandResult<PaymentResponse> handle(CreatePaymentCommand command) {
        try {
            log.info("Handling CreatePaymentCommand for order: {}", command.getOrderId());

            // 1. Check if payment already exists (idempotency)
            if (paymentService.existsByOrderId(command.getOrderId())) {
                log.warn("Payment already exists for order: {}", command.getOrderId());
                Payment existing = paymentRepository.findByOrderId(command.getOrderId())
                    .orElseThrow();
                return CommandResult.success(PaymentMapper.toResponse(existing));
            }

            // 2. Create payment
            Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(command.getOrderId())
                .userId(command.getUserId())
                .amount(command.getAmount())
                .method(command.getMethod())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(command.getInitiator())
                .version(0)
                .build();

            // 3. Validate business rules
            payment.validate();

            // 4. Process payment (simulate payment gateway call)
            boolean paymentSuccess = paymentService.processPayment(payment);

            // 5. Update timestamps
            payment.setUpdatedAt(LocalDateTime.now());
            payment.setUpdatedBy(command.getInitiator());

            // 6. Save payment
            Payment savedPayment = paymentRepository.save(payment);

            // 7. Record business metrics
            boolean paymentSuccessful = savedPayment.getStatus() == PaymentStatus.COMPLETED;
            businessMetricsService.recordPaymentProcessed(
                savedPayment.getUserId(),
                savedPayment.getAmount().doubleValue(),
                paymentSuccessful
            );
            businessMetricsService.recordUserActivity(savedPayment.getUserId());

            log.info("Payment created successfully: paymentId={}, status={}",
                    savedPayment.getId(), savedPayment.getStatus());

            return CommandResult.success(PaymentMapper.toResponse(savedPayment));

        } catch (IllegalArgumentException e) {
            log.error("Invalid payment: {}", e.getMessage());
            return CommandResult.failure(e.getMessage(), "INVALID_PAYMENT");

        } catch (Exception e) {
            log.error("Failed to create payment for order: {}", command.getOrderId(), e);
            return CommandResult.failure(
                "Failed to create payment: " + e.getMessage(),
                "PAYMENT_CREATION_FAILED"
            );
        }
    }
}

