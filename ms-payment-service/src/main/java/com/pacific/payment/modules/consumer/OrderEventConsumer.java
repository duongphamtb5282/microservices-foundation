package com.pacific.payment.modules.consumer;

import com.pacific.core.messaging.consumer.BaseEventConsumer;
import com.pacific.core.messaging.cqrs.event.DomainEvent;
import com.pacific.core.messaging.error.ErrorClassifier;
import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.monitoring.KafkaMetrics;
import com.pacific.core.messaging.retry.BackoffStrategy;
import com.pacific.core.messaging.retry.RetryContext;
import com.pacific.core.messaging.retry.RetryPolicy;
import com.pacific.core.messaging.retry.RetryStrategy;
import com.pacific.core.messaging.security.EncryptedEventWrapper;
import com.pacific.core.messaging.security.MessageEncryptionService;

import java.time.Duration;
import com.pacific.payment.modules.consumer.event.OrderCreatedEvent;
import com.pacific.payment.modules.consumer.handler.OrderCreatedEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events
 * Extends BaseEventConsumer from backend-core to get retry logic, metrics, and error handling
 */
@Component
@Slf4j
public class OrderEventConsumer {

    private final OrderCreatedEventHandler handler;
    private final MessageEncryptionService messageEncryptionService;
    private final RetryStrategy retryStrategy;
    private final ErrorClassifier errorClassifier;
    private final DeadLetterQueue deadLetterQueue;
    private final KafkaMetrics kafkaMetrics;
    private final BackoffStrategy backoffStrategy;

    public OrderEventConsumer(
            OrderCreatedEventHandler handler,
            MessageEncryptionService messageEncryptionService,
            RetryStrategy retryStrategy,
            ErrorClassifier errorClassifier,
            DeadLetterQueue deadLetterQueue,
            KafkaMetrics kafkaMetrics,
            BackoffStrategy backoffStrategy) {
        this.handler = handler;
        this.messageEncryptionService = messageEncryptionService;
        this.retryStrategy = retryStrategy;
        this.errorClassifier = errorClassifier;
        this.deadLetterQueue = deadLetterQueue;
        this.kafkaMetrics = kafkaMetrics;
        this.backoffStrategy = backoffStrategy;
    }

    @KafkaListener(
        topics = "${payment.messaging.order-events-topic}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(
            @Payload Object event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received event from topic: {}, partition: {}, offset: {}", topic, partition, offset);

        // Process event with retry logic
        processEventWithRetry(event, topic, partition, offset, acknowledgment);
    }

    /**
     * Process event with retry logic using the injected retry strategy.
     */
    private void processEventWithRetry(Object event, String topic, int partition, long offset, Acknowledgment acknowledgment) {
        String eventId = generateEventId(event);
        String correlationId = extractCorrelationId(event);

        log.info("Received event: {} from topic: {}, partition: {}, offset: {}, eventId: {}",
                event.getClass().getSimpleName(), topic, partition, offset, eventId);

        // Record consumption metrics
        kafkaMetrics.incrementEventsConsumed(topic, event.getClass().getSimpleName());

        // Create retry context
        RetryContext context = RetryContext.builder()
            .eventId(eventId)
            .topic(topic)
            .partition(partition)
            .offset(offset)
            .correlationId(correlationId)
            .startTime(java.time.Instant.now())
            .build();

        try {
            // Process with retry logic
            processWithRetry(event, context);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

            long processingTime = System.currentTimeMillis() - context.getStartTime().toEpochMilli();
            log.info("Event processed successfully: {} in {}ms", eventId, processingTime);

            // Record success metrics
            kafkaMetrics.recordEventProcessingTime(topic, event.getClass().getSimpleName(), processingTime);

        } catch (Exception e) {
            log.error("Failed to process event: {} after retries", eventId, e);

            // Handle failed event based on error classification
            handleFailedEvent(event, context, e, acknowledgment);
        }
    }

    /**
     * Process the event with retry logic.
     */
    private void processWithRetry(Object event, RetryContext context) {
        RetryPolicy policy = createRetryPolicy(context);

        try {
            retryStrategy.executeWithRetry(
                () -> {
                    try {
                        processEvent(event);
                        return true;
                    } catch (Exception e) {
                        log.warn("Event processing failed for event: {}, attempt: {}",
                                context.getEventId(), context.getAttemptCount() + 1, e);
                        throw e;
                    }
                },
                policy,
                context
            );
        } catch (com.pacific.core.messaging.retry.MaxRetriesExceededException e) {
            throw new RuntimeException("Max retries exceeded for event: " + context.getEventId(), e);
        }
    }

    /**
     * Handle failed event processing.
     */
    private void handleFailedEvent(Object event, RetryContext context, Exception e, Acknowledgment acknowledgment) {
        RetryPolicy retryPolicy = createRetryPolicy(context);
        if (errorClassifier.isRetryable(e, retryPolicy)) {
            // For retryable errors, don't acknowledge - let Kafka retry
            log.warn("Retryable error for event: {}, will retry", context.getEventId());
            throw new RuntimeException("Retryable error: " + e.getMessage(), e);
        } else {
            // For non-retryable errors, send to DLQ and acknowledge
            log.error("Non-retryable error for event: {}, sending to DLQ", context.getEventId());
            sendToDeadLetterQueue(event, context, e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Send failed event to Dead Letter Queue.
     */
    private void sendToDeadLetterQueue(Object event, RetryContext context, Exception e) {
        try {
            deadLetterQueue.send(context, e);
            log.info("Event sent to DLQ: {}", context.getEventId());
        } catch (Exception dlqException) {
            log.error("Failed to send event to DLQ: {}", context.getEventId(), dlqException);
        }
    }

    /**
     * Create retry policy based on context and configuration.
     */
    private RetryPolicy createRetryPolicy(RetryContext context) {
        RetryPolicy retryPolicy = RetryPolicy.builder()
            .maxAttempts(3)
            .initialBackoff(Duration.ofSeconds(1)) // Use fixed initial backoff
            .backoffMultiplier(2.0)
            .jitterFactor(0.1)
            .retryableExceptions(java.util.List.of(
                IllegalStateException.class,
                org.springframework.dao.DataAccessException.class,
                java.net.SocketTimeoutException.class
            ))
            .build();

        return retryPolicy;
    }

    private void processEvent(Object event) {
        try {
            // Handle both encrypted and non-encrypted events
            if (event instanceof EncryptedEventWrapper) {
                handleEncryptedEvent((EncryptedEventWrapper) event);
            } else if (event instanceof OrderCreatedEvent) {
                handleDirectEvent((OrderCreatedEvent) event);
            } else {
                log.warn("Unknown event type received: {}", event.getClass().getSimpleName());
                throw new IllegalArgumentException("Unsupported event type: " + event.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Failed to process event", e);
            throw e;
        }
    }

    /**
     * Handle encrypted event wrapper.
     */
    private void handleEncryptedEvent(EncryptedEventWrapper wrapper) {
        try {
            String topic = "order.events"; // Current topic

            if (wrapper.hasEncryptedData()) {
                log.debug("Processing encrypted event: {}", wrapper.getEventType());

                // Decrypt the event data
                OrderCreatedEvent decryptedEvent = messageEncryptionService.decryptEvent(
                    wrapper.getEncryptedData(),
                    OrderCreatedEvent.class,
                    topic
                );

                // Delegate to handler
                handler.handle(decryptedEvent);

                log.info("Successfully processed encrypted event for order: {}", wrapper.getAggregateId());
            } else {
                log.warn("Received encrypted wrapper with no data: {}", wrapper.getAggregateId());
            }

        } catch (Exception e) {
            log.error("Failed to handle encrypted event: {}", wrapper.getAggregateId(), e);
            throw e;
        }
    }

    /**
     * Handle direct (non-encrypted) event.
     */
    private void handleDirectEvent(OrderCreatedEvent event) {
        log.debug("Processing direct event for order: {}", event.getOrderId());

        // Delegate to handler
        handler.handle(event);

        log.info("Successfully processed direct event for order: {}", event.getOrderId());
    }

    /**
     * Generate unique event ID for idempotency tracking.
     */
    private String generateEventId(Object event) {
        // Try to extract ID from event if it has one
        try {
            if (event instanceof OrderCreatedEvent) {
                return ((OrderCreatedEvent) event).getOrderId();
            } else if (event instanceof EncryptedEventWrapper) {
                return ((EncryptedEventWrapper) event).getAggregateId();
            }
        } catch (Exception e) {
            // Fallback to hash-based ID
        }

        // Fallback to UUID based on event content
        return java.util.UUID.nameUUIDFromBytes(event.toString().getBytes()).toString();
    }

    /**
     * Extract correlation ID from event if available.
     */
    private String extractCorrelationId(Object event) {
        try {
            if (event instanceof OrderCreatedEvent) {
                // Try to get correlation ID from OrderCreatedEvent
                // For now, return null as it's not implemented in this event
                return null;
            } else if (event instanceof EncryptedEventWrapper) {
                return ((EncryptedEventWrapper) event).getCorrelationId();
            }
        } catch (Exception e) {
            // Fallback to null
        }

        return null;
    }
}

