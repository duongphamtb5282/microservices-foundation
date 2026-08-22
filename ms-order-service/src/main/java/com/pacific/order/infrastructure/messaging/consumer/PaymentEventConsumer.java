package com.pacific.order.infrastructure.messaging.consumer;

import com.pacific.order.application.handler.PaymentResultEventHandler;
import com.pacific.order.infrastructure.messaging.event.PaymentResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer for payment result events (F-02 saga return path). Manual ack: the offset commits only
 * after the order is settled, so a failed settlement is redelivered (at-least-once) — the handler
 * is idempotent (skips already-settled orders).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

  private final PaymentResultEventHandler handler;

  @KafkaListener(
      topics = "${order.messaging.payment-events-topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "paymentResultListenerContainerFactory")
  public void consumePaymentResult(
      @Payload PaymentResultEvent event, Acknowledgment acknowledgment) {
    try {
      handler.handle(event);
      acknowledgment.acknowledge();
      log.debug(
          "Payment result for order {} acknowledged (status: {})",
          event.getOrderId(),
          event.getStatus());
    } catch (Exception e) {
      // No ack -> Kafka redelivers. The handler is idempotent, so replays are safe.
      log.error(
          "Failed to settle order {} from payment result — will redeliver", event.getOrderId(), e);
      throw e;
    }
  }
}
