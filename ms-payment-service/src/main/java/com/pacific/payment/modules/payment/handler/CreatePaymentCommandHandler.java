package com.pacific.payment.modules.payment.handler;

import com.pacific.core.messaging.cqrs.command.CommandHandler;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.metrics.BusinessMetricsService;
import com.pacific.payment.modules.payment.command.CreatePaymentCommand;
import com.pacific.payment.modules.payment.domain.Payment;
import com.pacific.payment.modules.payment.domain.PaymentStatus;
import com.pacific.payment.modules.payment.dto.PaymentResponse;
import com.pacific.payment.modules.payment.event.PaymentResultEvent;
import com.pacific.payment.modules.payment.exception.PaymentNotFoundException;
import com.pacific.payment.modules.payment.mapper.PaymentMapper;
import com.pacific.payment.modules.payment.outbox.PaymentOutboxService;
import com.pacific.payment.modules.payment.repository.PaymentRepository;
import com.pacific.payment.modules.payment.service.PaymentService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Handler for CreatePaymentCommand Implements backend-core CommandHandler interface */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentCommandHandler
    implements CommandHandler<CreatePaymentCommand, PaymentResponse> {

  private final PaymentRepository paymentRepository;
  private final PaymentService paymentService;
  private final PaymentOutboxService outboxService;
  private final BusinessMetricsService businessMetricsService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public CommandResult<PaymentResponse> handle(CreatePaymentCommand command) {
    try {
      log.info("Handling CreatePaymentCommand for order: {}", command.getOrderId());

      // 1. Check if payment already exists (idempotency) — the unique(order_id) index backstops
      //    the check-then-act race; a losing insert raises DataIntegrityViolationException, the tx
      //    rolls back, and the retry replays this existing payment (idempotency-proposal.md P3).
      if (paymentService.existsByOrderId(command.getOrderId())) {
        log.warn("Payment already exists for order: {}", command.getOrderId());
        Payment existing =
            paymentRepository
                .findByOrderId(command.getOrderId())
                .orElseThrow(
                    () ->
                        new PaymentNotFoundException(
                            "Payment exists check passed but no row found for order "
                                + command.getOrderId()));
        return CommandResult.success(PaymentMapper.toResponse(existing));
      }

      // 2. Create payment
      Payment payment =
          Payment.builder()
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

      // 3. Validate business rules — scoped so an IllegalArgumentException is only treated as an
      //    INVALID_PAYMENT outcome here, before any write. A post-write IAE (mapper, metrics)
      //    must propagate and roll back instead of committing while the client sees failure
      //    (ADR-0012 FIX #2).
      try {
        payment.validate();
      } catch (IllegalArgumentException e) {
        log.error("Invalid payment: {}", e.getMessage());
        return CommandResult.failure(e.getMessage(), "INVALID_PAYMENT");
      }

      // 4. Process payment (simulate payment gateway call)
      boolean paymentSuccess = paymentService.processPayment(payment);

      // 5. Update timestamps
      payment.setUpdatedAt(LocalDateTime.now());
      payment.setUpdatedBy(command.getInitiator());

      // 6. Save payment
      Payment savedPayment = paymentRepository.save(payment);

      // 7. Saga return path (F-02): record the payment result in the SAME transaction via the
      //    outbox — the order service will settle the order (CONFIRMED/FAILED) from it. The
      //    replay path above never records again, so an order gets exactly one result event.
      if (savedPayment.getStatus() == PaymentStatus.COMPLETED) {
        outboxService.record(
            PaymentResultEvent.completed(
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getGatewayTransactionId(),
                command.getCorrelationId()));
      } else if (savedPayment.getStatus() == PaymentStatus.FAILED) {
        outboxService.record(
            PaymentResultEvent.failed(
                savedPayment.getId(),
                savedPayment.getOrderId(),
                savedPayment.getUserId(),
                savedPayment.getGatewayResponse(),
                command.getCorrelationId()));
      }

      // 8. Record business metrics
      boolean paymentSuccessful = savedPayment.getStatus() == PaymentStatus.COMPLETED;
      businessMetricsService.recordPaymentProcessed(
          savedPayment.getUserId(), savedPayment.getAmount().doubleValue(), paymentSuccessful);
      businessMetricsService.recordUserActivity(savedPayment.getUserId());

      log.info(
          "Payment created successfully: paymentId={}, status={}",
          savedPayment.getId(),
          savedPayment.getStatus());

      return CommandResult.success(PaymentMapper.toResponse(savedPayment));

    } catch (Exception e) {
      // F-25: propagate so @Transactional rolls back. Returning failure here would commit the
      // payment while the client sees an error — a retry would then replay an already-saved
      // payment and the saga result event would never fire.
      log.error(
          "Failed to create payment for order: {} — transaction will roll back",
          command.getOrderId(),
          e);
      throw e;
    }
  }
}
