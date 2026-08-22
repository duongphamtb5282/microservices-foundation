package com.pacific.payment.modules.consumer;

import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.error.ErrorClassifier;
import com.pacific.core.messaging.monitoring.KafkaMetrics;
import com.pacific.core.messaging.retry.BackoffStrategy;
import com.pacific.core.messaging.retry.RetryContext;
import com.pacific.core.messaging.retry.RetryPolicy;
import com.pacific.core.messaging.retry.RetryStrategy;
import com.pacific.payment.modules.consumer.event.OrderCreatedEvent;
import com.pacific.payment.modules.consumer.exception.EventRetryableException;
import com.pacific.payment.modules.consumer.handler.OrderCreatedEventHandler;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events Extends BaseEventConsumer from backend-core to get retry logic,
 * metrics, and error handling
 */
@Component
@Slf4j
public class OrderEventConsumer {

  private final OrderCreatedEventHandler handler;
  private final RetryStrategy retryStrategy;
  private final ErrorClassifier errorClassifier;
  private final DeadLetterQueue deadLetterQueue;
  private final KafkaMetrics kafkaMetrics;
  private final BackoffStrategy backoffStrategy;

  public OrderEventConsumer(
      OrderCreatedEventHandler handler,
      RetryStrategy retryStrategy,
      ErrorClassifier errorClassifier,
      DeadLetterQueue deadLetterQueue,
      KafkaMetrics kafkaMetrics,
      BackoffStrategy backoffStrategy) {
    this.handler = handler;
    this.retryStrategy = retryStrategy;
    this.errorClassifier = errorClassifier;
    this.deadLetterQueue = deadLetterQueue;
    this.kafkaMetrics = kafkaMetrics;
    this.backoffStrategy = backoffStrategy;
  }

  @KafkaListener(
      topics = "${payment.messaging.order-events-topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaListenerContainerFactory")
  public void consumeOrderEvent(
      @Payload OrderCreatedEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset,
      Acknowledgment acknowledgment) {

    log.info("Received event from topic: {}, partition: {}, offset: {}", topic, partition, offset);

    // Process event with retry logic
    processEventWithRetry(event, topic, partition, offset, acknowledgment);
  }

  /** Process event with retry logic using the injected retry strategy. */
  private void processEventWithRetry(
      OrderCreatedEvent event,
      String topic,
      int partition,
      long offset,
      Acknowledgment acknowledgment) {
    String eventId = event.getOrderId();

    log.info(
        "Received event: {} from topic: {}, partition: {}, offset: {}, eventId: {}",
        event.getClass().getSimpleName(),
        topic,
        partition,
        offset,
        eventId);

    // Record consumption metrics
    kafkaMetrics.incrementEventsConsumed(topic, event.getClass().getSimpleName());

    // Create retry context
    RetryContext context =
        RetryContext.builder()
            .eventId(eventId)
            .topic(topic)
            .partition(partition)
            .offset(offset)
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
      kafkaMetrics.recordEventProcessingTime(
          topic, event.getClass().getSimpleName(), processingTime);

    } catch (Exception e) {
      log.error("Failed to process event: {} after retries", eventId, e);

      // Handle failed event based on error classification
      handleFailedEvent(event, context, e, acknowledgment);
    }
  }

  /** Process the event with retry logic. */
  private void processWithRetry(OrderCreatedEvent event, RetryContext context) {
    RetryPolicy policy = createRetryPolicy(context);

    try {
      retryStrategy.executeWithRetry(
          () -> {
            try {
              processEvent(event);
              return true;
            } catch (Exception e) {
              log.warn(
                  "Event processing failed for event: {}, attempt: {}",
                  context.getEventId(),
                  context.getAttemptCount() + 1,
                  e);
              throw e;
            }
          },
          policy,
          context);
    } catch (com.pacific.core.messaging.retry.MaxRetriesExceededException e) {
      // Typed exception from backend-core — propagate as-is (no ack -> redelivery)
      throw e;
    }
  }

  /** Handle failed event processing. */
  private void handleFailedEvent(
      OrderCreatedEvent event, RetryContext context, Exception e, Acknowledgment acknowledgment) {
    RetryPolicy retryPolicy = createRetryPolicy(context);
    if (errorClassifier.isRetryable(e, retryPolicy)) {
      // For retryable errors, don't acknowledge - let Kafka retry
      log.warn("Retryable error for event: {}, will retry", context.getEventId());
      throw new EventRetryableException("Retryable error for event: " + context.getEventId(), e);
    } else {
      // For non-retryable errors, send to DLQ and acknowledge
      log.error("Non-retryable error for event: {}, sending to DLQ", context.getEventId());
      sendToDeadLetterQueue(event, context, e);
      acknowledgment.acknowledge();
    }
  }

  /** Send failed event to Dead Letter Queue. */
  private void sendToDeadLetterQueue(OrderCreatedEvent event, RetryContext context, Exception e) {
    try {
      deadLetterQueue.send(context, e);
      log.info("Event sent to DLQ: {}", context.getEventId());
    } catch (Exception dlqException) {
      log.error("Failed to send event to DLQ: {}", context.getEventId(), dlqException);
    }
  }

  /** Create retry policy based on context and configuration. */
  private RetryPolicy createRetryPolicy(RetryContext context) {
    return RetryPolicy.builder()
        .maxAttempts(3)
        .initialBackoff(Duration.ofSeconds(1)) // Use fixed initial backoff
        .backoffMultiplier(2.0)
        .jitterFactor(0.1)
        .retryableExceptions(
            java.util.List.of(
                IllegalStateException.class,
                org.springframework.dao.DataAccessException.class,
                java.net.SocketTimeoutException.class))
        .build();
  }

  /**
   * Delegate the event to the handler. DE-2: the EncryptedEventWrapper branch was unreachable (the
   * consumer deserializes straight into OrderCreatedEvent, USE_TYPE_INFO_HEADERS=false) and event
   * encryption has been removed.
   */
  private void processEvent(OrderCreatedEvent event) {
    handler.handle(event);
    log.info("Successfully processed order event for order: {}", event.getOrderId());
  }
}
