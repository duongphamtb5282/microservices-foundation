package com.pacific.core.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Correlation-aware error handler for Kafka listeners.
 *
 * <p>Kafka is always present (no feature flag), so this implements {@link CommonErrorHandler}
 * directly — no POJO-proxy indirection needed. Errors are logged and treated as handled; the
 * container's ack policy (MANUAL_IMMEDIATE) decides whether the record is committed.
 */
@Slf4j
@Component("correlationAwareErrorHandler")
@ConditionalOnClass(CommonErrorHandler.class)
public class CorrelationAwareErrorHandler implements CommonErrorHandler {

  @Override
  public boolean handleOne(
      Exception thrownException,
      ConsumerRecord<?, ?> record,
      Consumer<?, ?> consumer,
      MessageListenerContainer container) {
    log.error("Kafka listener error: {}", thrownException.toString());
    return true; // handled — do not rethrow
  }

  @Override
  public void handleOtherException(
      Exception thrownException,
      Consumer<?, ?> consumer,
      MessageListenerContainer container,
      boolean batchListener) {
    log.error("Kafka consumer error: {}", thrownException.getMessage());
  }
}
