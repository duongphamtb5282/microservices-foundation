package com.pacific.order.infrastructure.messaging.publisher;

import com.pacific.core.messaging.cqrs.event.EventPublisher;
import com.pacific.core.messaging.security.EncryptedEventWrapper;
import com.pacific.core.messaging.security.MessageEncryptionService;
import com.pacific.order.domain.event.OrderCreatedEvent;
import com.pacific.order.infrastructure.messaging.config.OrderMessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publisher for order-related events
 * Uses backend-core EventPublisher
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final EventPublisher eventPublisher;  // From backend-core
    private final MessageEncryptionService messageEncryptionService;  // For event encryption
    private final OrderMessagingProperties properties;

    /**
     * Publish OrderCreatedEvent to Kafka with encryption
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        String topic = properties.getOrderEventsTopic();

        log.info("Publishing OrderCreatedEvent to topic: {} (orderId: {})",
                topic, event.getOrderId());

        try {
            // Encrypt event data if it contains sensitive information
            String encryptedEventData = messageEncryptionService.encryptEvent(event, topic);

            // Create encrypted event wrapper
            EncryptedEventWrapper wrapper = EncryptedEventWrapper.builder()
                .eventType(event.getEventType())
                .aggregateId(event.getAggregateId())
                .correlationId(event.getCorrelationId())
                .encryptedData(encryptedEventData)
                .timestamp(java.time.Instant.now())
                .encryptionEnabled(true)
                .build();

            // Publish encrypted wrapper
            eventPublisher.publish(topic, event.getOrderId(), wrapper)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCreatedEvent: {}",
                                 event.getOrderId(), ex);
                    } else {
                        log.info("OrderCreatedEvent published successfully: {} (partition: {}, offset: {})",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

        } catch (Exception e) {
            log.error("Failed to encrypt and publish OrderCreatedEvent: {}", event.getOrderId(), e);

            // Fallback: publish without encryption in case of encryption failure
            log.warn("Publishing OrderCreatedEvent without encryption as fallback");
            eventPublisher.publish(topic, event.getOrderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish fallback OrderCreatedEvent: {}",
                                 event.getOrderId(), ex);
                    } else {
                        log.info("Fallback OrderCreatedEvent published successfully: {} (partition: {}, offset: {})",
                                event.getOrderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
        }
    }
}

