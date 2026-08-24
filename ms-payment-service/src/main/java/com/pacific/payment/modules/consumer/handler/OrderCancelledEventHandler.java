package com.pacific.payment.modules.consumer.handler;

import com.pacific.payment.modules.consumer.event.OrderCancelledEvent;
import com.pacific.payment.modules.payment.domain.Payment;
import com.pacific.payment.modules.payment.domain.PaymentStatus;
import com.pacific.payment.modules.payment.event.PaymentResultEvent;
import com.pacific.payment.modules.payment.outbox.PaymentOutboxService;
import com.pacific.payment.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga compensation handler (ADR-0007): a cancelled order must not leave money taken. If the
 * payment was COMPLETED, it is refunded (REFUNDED) and a PaymentResultEvent("REFUNDED") is recorded
 * into the payment outbox in the SAME transaction so the order service records the refund outcome.
 * If no money moved (PENDING/PROCESSING), the payment is cancelled instead. A payment that never
 * existed is skipped: per-partition ordering on the orderId key guarantees the ORDER_CREATED event
 * was acked (or DLQed) before this event is delivered, so a missing payment means none was ever
 * created. Idempotent: duplicate cancellation events (at-least-once) are ignored.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledEventHandler {

  private final PaymentRepository paymentRepository;
  private final PaymentOutboxService outboxService;

  @Transactional
  public void handle(OrderCancelledEvent event) {
    log.info(
        "Processing OrderCancelledEvent: orderId={}, reason={}",
        event.getOrderId(),
        event.getReason());

    Payment payment = paymentRepository.findByOrderId(event.getOrderId()).orElse(null);
    if (payment == null) {
      // Per-key ordering (see class javadoc): a missing payment means the payment was never
      // created — nothing to refund (ADR-0007).
      log.warn("No payment for cancelled order {} — nothing to refund", event.getOrderId());
      return;
    }

    // Idempotency: duplicate cancel events (at-least-once) must not re-refund or re-cancel.
    if (payment.getStatus() == PaymentStatus.REFUNDED
        || payment.getStatus() == PaymentStatus.CANCELLED) {
      log.warn(
          "Payment {} for order {} already {} — ignoring duplicate cancellation",
          payment.getId(),
          event.getOrderId(),
          payment.getStatus());
      return;
    }

    if (payment.getStatus() == PaymentStatus.COMPLETED) {
      // Money moved — refund it and tell the order service in the SAME transaction (outbox).
      payment.markRefunded();
      paymentRepository.save(payment);
      outboxService.record(
          PaymentResultEvent.refunded(
              payment.getId(),
              payment.getOrderId(),
              payment.getUserId(),
              payment.getGatewayTransactionId(),
              event.getCorrelationId()));
      log.info("Payment {} refunded for cancelled order {}", payment.getId(), event.getOrderId());
    } else if (payment.getStatus() == PaymentStatus.PENDING
        || payment.getStatus() == PaymentStatus.PROCESSING) {
      // No money moved yet — surface the cancellation, nothing to refund.
      payment.markCancelled();
      paymentRepository.save(payment);
      log.info(
          "Payment {} cancelled (no refund needed) for order {}",
          payment.getId(),
          event.getOrderId());
    } else {
      // FAILED — terminal, no money moved; nothing to do.
      log.info(
          "Payment {} for cancelled order {} is {} — nothing to refund",
          payment.getId(),
          event.getOrderId(),
          payment.getStatus());
    }
  }
}
