package com.pacific.customer.service;

import com.pacific.shared.events.UserCreatedEvent;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer service that listens for user registration events from auth-service and creates
 * corresponding customer records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

  private static final String USER_EVENTS_TOPIC = "user-events";
  private static final String CORRELATION_ID_MDC_KEY = "correlationId";

  private final CustomerService customerService;

  /**
   * Listens to user registration events and creates customer records. Uses manual acknowledgment
   * for reliable message processing.
   */
  @KafkaListener(
      topics = USER_EVENTS_TOPIC,
      groupId = "customer-service-group",
      containerFactory = "kafkaListenerContainerFactory")
  @Timed(
      value = "customer.user_event_consumed",
      description = "Time taken to process user registration events",
      histogram = true)
  public void consumeUserCreatedEvent(
      @Payload UserCreatedEvent event,
      @Header(KafkaHeaders.RECEIVED_KEY) String key,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
      @Header(KafkaHeaders.OFFSET) String offset,
      @Header(value = "correlationId", required = false) String correlationId,
      Acknowledgment acknowledgment) {

    // Set correlation ID in MDC for tracing
    if (correlationId != null && !correlationId.trim().isEmpty()) {
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    } else {
      correlationId =
          event.getCorrelationId() != null
              ? event.getCorrelationId()
              : java.util.UUID.randomUUID().toString();
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }

    log.info(
        "🎯 Received UserCreatedEvent - User ID: {}, Username: {}, Email: {}, Topic: {}, Partition: {}, Offset: {}, Correlation ID: {}",
        event.getUserId(),
        event.getUsername(),
        event.getEmail(),
        topic,
        partition,
        offset,
        correlationId);

    // Create final local variables for lambda usage
    final String finalUserId = event.getUserId();
    final String finalCorrelationId = correlationId;

    try {
      // Process the user registration event by creating a customer
      // Since this is reactive, we subscribe to the Mono and handle completion
      customerService
          .createCustomerFromUserEvent(event, correlationId)
          .doOnSuccess(
              v -> {
                // Acknowledge successful processing
                acknowledgment.acknowledge();
                log.info(
                    "✅ Successfully processed UserCreatedEvent and created customer - User ID: {}, Correlation ID: {}",
                    finalUserId,
                    finalCorrelationId);
              })
          .doOnError(
              error -> {
                log.error(
                    "❌ Failed to create customer from UserCreatedEvent - User ID: {}, Correlation ID: {}, Error: {}. "
                        + "Offset NOT acknowledged - Kafka will redeliver the message (at-least-once).",
                    finalUserId,
                    finalCorrelationId,
                    error.getMessage(),
                    error);
                // Do NOT acknowledge on failure: leaving the offset uncommitted makes Kafka
                // redeliver the message (at-least-once). Acknowledging here would permanently
                // drop the event. No DLQ/retry mechanism exists in this module yet, so
                // redelivery is the current at-least-once guarantee.
              })
          .subscribe(); // Subscribe to start the reactive chain

    } catch (Exception e) {
      log.error(
          "❌ Failed to process UserCreatedEvent - User ID: {}, Correlation ID: {}, Error: {}. "
              + "Offset NOT acknowledged - Kafka will redeliver the message (at-least-once).",
          event.getUserId(),
          correlationId,
          e.getMessage(),
          e);

      // Do NOT acknowledge on failure: leaving the offset uncommitted makes Kafka redeliver the
      // message (at-least-once). Acknowledging here would permanently drop the event.

      // TODO: Implement proper error handling - dead letter queue (customer-events.dlq topic is
      // already defined in application.yml) or a bounded retry mechanism.
    } finally {
      // Clean up MDC
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }
}
