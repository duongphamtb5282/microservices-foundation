package com.pacific.core.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id:default-group}")
  private String groupId;

  @Value("${spring.kafka.consumer.type-mappings:}")
  private String typeMappings;

  @Value("${spring.kafka.consumer.value-default-type:}")
  private String valueDefaultType;

  @Bean
  public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reliability
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
    props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 40000);
    props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
    props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 55296000); // 50MB
    props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 10485760); // 10MB
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "backend-core-consumer");

    // JSON Deserializer configuration
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

    // Configure type mappings if provided (removes hardcoded shared library dependency)
    if (typeMappings != null && !typeMappings.trim().isEmpty()) {
      props.put(JsonDeserializer.TYPE_MAPPINGS, typeMappings);
    }

    // Per-service fallback target class. Records written before 2026-08-25 carry no __TypeId__
    // header (JsonSerializer.ADD_TYPE_INFO_HEADERS was disabled in core's producer config), so the
    // JsonDeserializer could not pick a class and every such record failed with "Error
    // deserializing VALUE ... no type information in headers" — an endless error-handler loop that
    // pinned the partition at the first offset. A default type lets those headerless records decode
    // (they are valid JSON of the service's only event type, e.g. UserCreatedEvent on user-events)
    // and doubles as a poison-pill safety net. Services opt in via
    // spring.kafka.consumer.value-default-type; when a __TypeId__ header IS present it still wins.
    if (valueDefaultType != null && !valueDefaultType.trim().isEmpty()) {
      props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueDefaultType);
    }

    // Security configuration (if needed)
    // props.put(SaslConfigs.SASL_MECHANISM_CONFIG, "PLAIN");
    // props.put(SaslConfigs.SASL_JAAS_CONFIG, "...");

    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
      CommonErrorHandler errorHandler) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.setConcurrency(3); // Number of consumer threads
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
    factory.setCommonErrorHandler(errorHandler);
    return factory;
  }
}
