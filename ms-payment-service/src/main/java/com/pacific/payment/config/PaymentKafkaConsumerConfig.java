package com.pacific.payment.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Kafka consumer configuration. The order-events topic carries both ORDER_CREATED and
 * ORDER_CANCELLED payloads (ADR-0007), so the value deserializer is a plain String and
 * {@code OrderEventConsumer} dispatches on the eventType field.
 *
 * <p>Named {@code PaymentKafkaConsumerConfig} (not {@code KafkaConsumerConfig}) to avoid a bean-name
 * clash with {@code com.pacific.core.config.KafkaConsumerConfig}, which this service's
 * {@code @ComponentScan("com.pacific.core")} also pulls in — two same-named {@code @Configuration}
 * classes fail startup with a ConflictingBeanDefinitionException. The beans are also renamed
 * (payment* prefix) so they coexist with core's generic {@code consumerFactory} /
 * {@code kafkaListenerContainerFactory}: core's Object-typed factory serves
 * {@code CorrelationAwareConsumer}, this String-typed factory serves {@code OrderEventConsumer}
 * (which binds via {@code containerFactory = "paymentKafkaListenerContainerFactory"}).
 */
@Configuration
@EnableKafka
public class PaymentKafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  @Bean
  public ConsumerFactory<String, String> paymentConsumerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

    return new DefaultKafkaConsumerFactory<>(config);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String>
      paymentKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(paymentConsumerFactory());
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
    factory.setConcurrency(3); // Number of consumer threads
    return factory;
  }
}
