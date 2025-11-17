package com.pacific.shared.config;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.pacific.shared.events.BaseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/** Kafka configuration for Spring Cloud Stream */
@Slf4j
@Configuration
public class KafkaConfig {

  /** Configure ObjectMapper for JSON serialization */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  /** Message transformer for adding headers */
  @Bean
  public Function<BaseEvent, Message<BaseEvent>> eventTransformer() {
    return event -> {
      log.debug("Transforming event {} for publishing", event.getEventType());
      return MessageBuilder.withPayload(event)
          .setHeader("eventType", event.getEventType())
          .setHeader("eventId", event.getEventId())
          .setHeader("source", event.getSource())
          .setHeader("version", event.getVersion())
          .setHeader("timestamp", event.getTimestamp())
          .build();
    };
  }
}
