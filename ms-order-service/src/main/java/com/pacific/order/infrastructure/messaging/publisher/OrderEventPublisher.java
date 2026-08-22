package com.pacific.order.infrastructure.messaging.publisher;

import com.pacific.core.messaging.cqrs.event.EventPublisher;
import com.pacific.order.domain.event.OrderCreatedEvent;
import com.pacific.order.infrastructure.messaging.config.OrderMessagingProperties;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

/** Publisher for order-related events Uses backend-core EventPublisher */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

  private final EventPublisher eventPublisher; // From backend-core
  private final OrderMessagingProperties properties;

  /**
   * Publish OrderCreatedEvent to Kafka. DE-2: event encryption removed - the event is published
   * plaintext (VPC + TLS secure the channel); consumers deserialize OrderCreatedEvent directly.
   *
   * <p>Returns the send future so the outbox relay (ADR-0001) can mark the row PUBLISHED only after
   * the broker acknowledges the send.
   */
  public CompletableFuture<SendResult<String, OrderCreatedEvent>> publishOrderCreated(
      OrderCreatedEvent event) {
    String topic = properties.getOrderEventsTopic();

    log.info("Publishing OrderCreatedEvent to topic: {} (orderId: {})", topic, event.getOrderId());

    CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
        eventPublisher.publish(topic, event.getOrderId(), event);

    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish OrderCreatedEvent: {}", event.getOrderId(), ex);
          } else {
            log.info(
                "OrderCreatedEvent published successfully: {} (partition: {}, offset: {})",
                event.getOrderId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });

    return future;
  }
}
