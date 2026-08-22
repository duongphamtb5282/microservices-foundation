package com.pacific.payment.modules.payment.outbox;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ObjectMapper for payment outbox payloads. Round-trips {@link PaymentResultEvent} (ISO-8601 string
 * timestamp, so no JavaTimeModule needed); payloads may gain fields over time, so unknown fields
 * must not fail the relay's read of old rows.
 */
@Configuration
public class PaymentOutboxConfig {

  @Bean
  public ObjectMapper paymentOutboxObjectMapper() {
    return new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }
}
