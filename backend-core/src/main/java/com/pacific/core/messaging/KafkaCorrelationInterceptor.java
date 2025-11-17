package com.pacific.core.messaging;

import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaCorrelationInterceptor implements ProducerInterceptor<String, Object> {

  private static final String CORRELATION_ID_HEADER_KEY = "correlationId";
  private static final String CORRELATION_ID_MDC_KEY = "correlationId";

  @Override
  public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
    // Extract correlation ID from current MDC context
    String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);

    // If no correlation ID in MDC, generate one (defensive)
    if (correlationId == null || correlationId.trim().isEmpty()) {
      correlationId = UUID.randomUUID().toString();
      // Optionally set it back to MDC for consistency in this request
      MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
    }

    // Create a copy of the record with correlation ID header
    ProducerRecord<String, Object> recordWithCorrelation =
        new ProducerRecord<>(
            record.topic(),
            record.partition(),
            record.timestamp(),
            record.key(),
            record.value(),
            record.headers());

    // Add correlation ID to message headers
    recordWithCorrelation.headers().add(CORRELATION_ID_HEADER_KEY, correlationId.getBytes());

    // Optional: Add source service information for better tracing
    recordWithCorrelation.headers().add("sourceService", "backend-core".getBytes());

    // Optional: Add timestamp for message ordering
    recordWithCorrelation
        .headers()
        .add("messageTimestamp", String.valueOf(System.currentTimeMillis()).getBytes());

    return recordWithCorrelation;
  }

  @Override
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    String correlationId = MDC.get(CORRELATION_ID_MDC_KEY);

    if (correlationId != null && !correlationId.trim().isEmpty()) {
      if (exception == null) {
        // Message sent successfully
        // You can add metrics here if needed
        // e.g., meterRegistry.counter("kafka.messages.sent",
        //     Tags.of("topic", metadata.topic(), "correlation_id", correlationId)).increment();
      } else {
        // Message send failed
        // e.g., meterRegistry.counter("kafka.messages.failed",
        //     Tags.of("topic", metadata.topic(), "correlation_id", correlationId, "error",
        // exception.getClass().getSimpleName())).increment();
      }
    }
  }

  @Override
  public void close() {
    // Cleanup resources if needed
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // Configuration initialization if needed
  }
}
