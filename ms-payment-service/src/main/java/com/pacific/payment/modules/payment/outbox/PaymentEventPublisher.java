package com.pacific.payment.modules.payment.outbox;

import com.pacific.core.messaging.cqrs.event.EventPublisher;
import com.pacific.payment.modules.payment.event.PaymentResultEvent;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/**
 * Publisher for payment result events (F-02 saga return path). Returns the send future so the
 * outbox relay can mark the row PUBLISHED only after the broker acknowledges the send.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

  private final EventPublisher eventPublisher; // From backend-core

  @Value("${payment.messaging.payment-events-topic}")
  private String paymentEventsTopic;

  public CompletableFuture<SendResult<String, PaymentResultEvent>> publishPaymentResult(
      PaymentResultEvent event) {
    String topic = paymentEventsTopic;

    log.info(
        "Publishing PaymentResultEvent to topic: {} (orderId: {}, status: {})",
        topic,
        event.getOrderId(),
        event.getStatus());

    CompletableFuture<SendResult<String, PaymentResultEvent>> future =
        eventPublisher.publish(topic, event.getOrderId(), event);

    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish PaymentResultEvent: {}", event.getOrderId(), ex);
          } else {
            log.info(
                "PaymentResultEvent published successfully: {} (partition: {}, offset: {})",
                event.getOrderId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });

    return future;
  }
}
