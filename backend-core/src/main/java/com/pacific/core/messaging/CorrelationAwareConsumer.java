package com.pacific.core.messaging;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "kafka.consumers.enabled",
    havingValue = "true",
    matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class CorrelationAwareConsumer {

  private static final String CORRELATION_ID_HEADER_KEY = "correlationId";
  private static final String CORRELATION_ID_MDC_KEY = "correlationId";

  private final ObjectMapper objectMapper;
  private final ExecutorService executor =
      Executors.newFixedThreadPool(
          4,
          r -> {
            Thread t = new Thread(r);
            t.setName("kafka-consumer-thread-" + t.getId());
            return t;
          });

  @KafkaListener(
      topics = {"customer-events", "order-events", "payment-events"},
      groupId = "event-processors",
      containerFactory = "kafkaListenerContainerFactory",
      errorHandler = "correlationAwareErrorHandler")
  public void handleEvents(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
    // Extract correlation ID from Kafka headers (robust extraction)
    String correlationId = extractCorrelationIdFromRecord(record);

    // Set MDC context for this processing thread
    if (correlationId != null && !correlationId.trim().isEmpty()) {
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    } else {
      // Fallback: generate new correlation ID for consumer processing
      correlationId = UUID.randomUUID().toString();
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
      log.warn("No correlation ID found in Kafka message, generated new one: {}", correlationId);
    }

    long startTime = System.currentTimeMillis();
    boolean processingSuccess = false;

    try {
      log.info(
          "Starting event processing. Topic: {}, Partition: {}, Offset: {}, Key: {}, Correlation ID: {}",
          record.topic(),
          record.partition(),
          record.offset(),
          record.key(),
          correlationId);

      // Process the event (synchronous processing for simplicity)
      // For complex processing, you can submit to executor with MDC context
      processEventSynchronous(record, correlationId);

      processingSuccess = true;
      log.debug(
          "Event processing completed successfully. Duration: {}ms, Correlation ID: {}",
          System.currentTimeMillis() - startTime,
          correlationId);

    } catch (Exception e) {
      log.error(
          "Event processing failed. Topic: {}, Offset: {}, Correlation ID: {}, Error: {}",
          record.topic(),
          record.offset(),
          correlationId,
          e.getMessage(),
          e);
      processingSuccess = false;
      // Don't rethrow - let error handler manage retries/DLQ
    } finally {
      // Manual acknowledgment based on processing result
      if (processingSuccess) {
        acknowledgment.acknowledge();
        log.debug("Message acknowledged successfully. Correlation ID: {}", correlationId);
      } else {
        // Don't acknowledge - will be retried by container or sent to DLQ
        log.warn(
            "Message not acknowledged due to processing failure. Correlation ID: {}",
            correlationId);
      }

      // Always clean up MDC to prevent context leakage between messages
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }

  /**
   * Synchronous event processing - all MDC context is preserved For async processing, use
   * processEventAsynchronous() method
   */
  private void processEventSynchronous(
      ConsumerRecord<String, Object> record, String correlationId) {
    try {
      // All operations in this method have correlation context available:
      // - Logging includes correlation ID
      // - Metrics include correlation_id tag
      // - HTTP calls propagate correlation ID via RestTemplateInterceptor
      // - Database operations can be logged with correlation context

      String eventType = determineEventType(record);
      Object eventData = record.value();

      switch (eventType) {
        case "CUSTOMER_CREATED":
          handleCustomerCreatedEvent(eventData, correlationId);
          break;
        case "ORDER_PLACED":
          handleOrderPlacedEvent(eventData, correlationId);
          break;
        case "PAYMENT_PROCESSED":
          handlePaymentProcessedEvent(eventData, correlationId);
          break;
        default:
          log.warn("Unknown event type: {}. Correlation ID: {}", eventType, correlationId);
          // Still process as generic event if needed
          handleGenericEvent(eventData, correlationId);
      }

      // Record processing metrics (optional)
      // kafkaMetrics.recordMessageProcessingDuration(record.topic(),
      //     System.currentTimeMillis() - startTime);

    } catch (Exception e) {
      log.error("Error in synchronous event processing. Correlation ID: {}", correlationId, e);
      throw e; // Re-throw to trigger error handling
    }
  }

  /**
   * Asynchronous event processing with full MDC context propagation Use this for long-running or
   * I/O heavy processing
   */
  private void processEventAsynchronous(
      ConsumerRecord<String, Object> record, String correlationId) {
    // Capture current MDC context for propagation to child thread
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    executor.submit(
        () -> {
          try {
            // Restore full MDC context in child thread
            if (mdcContext != null) {
              mdcContext.forEach(MDC::put);
            } else {
              // Ensure correlation ID is at least available
              MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            }

            log.info(
                "Async processing started. Topic: {}, Correlation ID: {}",
                record.topic(),
                correlationId);

            // Perform async processing
            processEventSynchronous(record, correlationId);

            log.debug("Async processing completed. Correlation ID: {}", correlationId);

          } catch (Exception e) {
            log.error("Async processing failed. Correlation ID: {}", correlationId, e);
            // Could trigger specific async error handling
          } finally {
            // Clean up MDC in child thread
            MDC.clear();
          }
        });
  }

  private String determineEventType(ConsumerRecord<String, Object> record) {
    // Extract event type from message content or headers
    if (record.key() != null) {
      if (record.key().contains("customer")) return "CUSTOMER_CREATED";
      if (record.key().contains("order")) return "ORDER_PLACED";
      if (record.key().contains("payment")) return "PAYMENT_PROCESSED";
    }

    // Fallback: check headers or payload
    var eventTypeHeader = record.headers().lastHeader("eventType");
    if (eventTypeHeader != null) {
      return new String(eventTypeHeader.value());
    }

    return "UNKNOWN";
  }

  private String extractCorrelationIdFromRecord(ConsumerRecord<String, Object> record) {
    try {
      // Primary: Check Kafka headers
      if (record.headers() != null) {
        var correlationHeader = record.headers().lastHeader(CORRELATION_ID_HEADER_KEY);
        if (correlationHeader != null) {
          String headerValue = new String(correlationHeader.value()).trim();
          if (!headerValue.isEmpty()) {
            return headerValue;
          }
        }
      }

      // Secondary: Check message payload for correlationId field (JSON)
      if (record.value() instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) record.value();
        if (payload.containsKey("correlationId")) {
          Object correlationObj = payload.get("correlationId");
          if (correlationObj instanceof String) {
            String payloadCorrelationId = ((String) correlationObj).trim();
            if (!payloadCorrelationId.isEmpty()) {
              return payloadCorrelationId;
            }
          }
        }
      }

      // Tertiary: Generate new one if completely missing
      return null; // Will trigger generation in main method

    } catch (Exception e) {
      log.warn("Error extracting correlation ID from record: {}", e.getMessage());
      return null; // Fallback to generation
    }
  }

  // Event handlers with correlation context
  private void handleCustomerCreatedEvent(Object eventData, String correlationId) {
    log.info(
        "Processing customer created event. Data: {}, Correlation ID: {}",
        eventData,
        correlationId);

    // Example business logic:
    // 1. Update analytics database
    // 2. Send welcome email (async)
    // 3. Notify other services via HTTP (correlation ID propagated automatically)
    // 4. All logs and metrics include correlation ID

    // Example downstream HTTP call (correlation ID will be propagated by RestTemplateInterceptor)
    // restTemplate.postForEntity("http://notification-service/api/customers", eventData);
  }

  private void handleOrderPlacedEvent(Object eventData, String correlationId) {
    log.info("Processing order placed event. Correlation ID: {}", correlationId);
    // Similar processing with full correlation context
  }

  private void handlePaymentProcessedEvent(Object eventData, String correlationId) {
    log.info("Processing payment processed event. Correlation ID: {}", correlationId);
    // Payment-specific processing
  }

  private void handleGenericEvent(Object eventData, String correlationId) {
    log.info("Processing generic event. Data: {}, Correlation ID: {}", eventData, correlationId);
    // Generic event handling
  }
}
