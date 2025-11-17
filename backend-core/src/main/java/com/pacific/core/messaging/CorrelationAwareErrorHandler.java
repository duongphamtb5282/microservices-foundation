package com.pacific.core.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/** Correlation-aware error handler for Kafka listeners */
@Slf4j
@Component("correlationAwareErrorHandler")
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class CorrelationAwareErrorHandler implements KafkaListenerErrorHandler, CommonErrorHandler {

  @Override
  public Object handleError(Message<?> message, ListenerExecutionFailedException exception) {
    log.error("Kafka listener error: {}", exception.getMessage());
    // Return null to indicate the error was handled and should not be rethrown
    return null;
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
