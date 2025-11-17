package com.pacific.core.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.pacific.core.messaging.KafkaCorrelationInterceptor;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaProducerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.producer.type-mappings:}")
  private String typeMappings;

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
    configProps.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 50);
    configProps.put(ProducerConfig.RETRY_BACKOFF_MAX_MS_CONFIG, 1000);
    configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
    configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

    // Configure type mappings if provided (removes hardcoded shared library dependency)
    if (typeMappings != null && !typeMappings.trim().isEmpty()) {
      configProps.put(JsonSerializer.TYPE_MAPPINGS, typeMappings);
    }

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public KafkaCorrelationInterceptor kafkaCorrelationInterceptor() {
    return new KafkaCorrelationInterceptor();
  }
}
