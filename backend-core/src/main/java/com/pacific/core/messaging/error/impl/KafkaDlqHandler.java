package com.pacific.core.messaging.error.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.config.KafkaWrapperProperties;
import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.error.DlqMessage;
import com.pacific.core.messaging.error.DlqStats;
import com.pacific.core.messaging.retry.RetryContext;

/**
 * Kafka-based implementation of Dead Letter Queue. Sends failed messages to dedicated DLQ topics.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class KafkaDlqHandler implements DeadLetterQueue {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaWrapperProperties properties;

  // Simple counters for stats (in production, use metrics system)
  private final AtomicLong totalMessages = new AtomicLong(0);
  private final AtomicLong reprocessedSuccessfully = new AtomicLong(0);
  private final AtomicLong reprocessedFailed = new AtomicLong(0);

  @Override
  public void send(RetryContext context, Throwable exception) {
    String dlqTopic = getDlqTopic(context.getTopic());

    log.info(
        "Sending message {} to DLQ topic: {} (attempts: {}, error: {})",
        context.getMessageId(),
        dlqTopic,
        context.getAttemptNumber(),
        exception.getClass().getSimpleName());

    DlqMessage dlqMessage =
        DlqMessage.builder()
            .messageId(context.getMessageId())
            .originalTopic(context.getTopic())
            .partition(context.getPartition())
            .offset(context.getOffset())
            .attemptNumber(context.getAttemptNumber())
            .firstAttemptTime(context.getFirstAttemptTime())
            .lastAttemptTime(context.getLastAttemptTime())
            .exceptionClass(exception.getClass().getName())
            .exceptionMessage(exception.getMessage())
            .stackTrace(getStackTrace(exception))
            .originalPayload(context.getOriginalPayload())
            .timestamp(Instant.now())
            .build();

    Message<DlqMessage> message =
        MessageBuilder.withPayload(dlqMessage)
            .setHeader(KafkaHeaders.TOPIC, dlqTopic)
            .setHeader(KafkaHeaders.KEY, context.getMessageId())
            .setHeader("original-topic", context.getTopic())
            .setHeader("exception-class", exception.getClass().getName())
            .setHeader("attempt-number", context.getAttemptNumber())
            .build();

    try {
      kafkaTemplate.send(message).get();
      totalMessages.incrementAndGet();

      log.info("Successfully sent message {} to DLQ topic: {}", context.getMessageId(), dlqTopic);

    } catch (Exception e) {
      log.error("Failed to send message {} to DLQ topic: {}", context.getMessageId(), dlqTopic, e);
    }
  }

  @Override
  public Optional<DlqMessage> retrieve(String messageId) {
    log.debug("Retrieving message {} from DLQ", messageId);
    // In a full implementation, you would consume from DLQ topic
    // For now, this is a placeholder
    return Optional.empty();
  }

  @Override
  public void reprocess(String messageId) {
    log.info("Reprocessing message {} from DLQ", messageId);

    // In a full implementation:
    // 1. Retrieve message from DLQ
    // 2. Send to original topic for reprocessing
    // 3. Track success/failure

    // Placeholder for now
    log.warn("DLQ reprocessing not yet implemented");
  }

  @Override
  public DlqStats getStats() {
    return DlqStats.builder()
        .totalMessages(totalMessages.get())
        .retryRate(0.0) // Would calculate from metrics
        .reprocessedSuccessfully(reprocessedSuccessfully.get())
        .reprocessedFailed(reprocessedFailed.get())
        .build();
  }

  /** Get DLQ topic name for original topic. */
  private String getDlqTopic(String originalTopic) {
    return originalTopic + properties.getRetry().getDlqTopicSuffix();
  }

  /** Get stack trace as string. */
  private String getStackTrace(Throwable exception) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    exception.printStackTrace(pw);
    return sw.toString();
  }
}
