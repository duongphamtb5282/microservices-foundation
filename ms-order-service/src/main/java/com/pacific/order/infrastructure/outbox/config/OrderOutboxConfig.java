package com.pacific.order.infrastructure.outbox.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Currency;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ObjectMapper for outbox payloads. Round-trips the domain events (Money is a @Value class holding
 * java.util.Currency, which Jackson cannot deserialize on its own).
 */
@Configuration
public class OrderOutboxConfig {

  @Bean
  public ObjectMapper orderOutboxObjectMapper() {
    return new ObjectMapper()
        .findAndRegisterModules() // JavaTimeModule + ParameterNamesModule (Money ctor)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .registerModule(currencyCodeModule());
  }

  /** Serialize Currency as its ISO code string so it round-trips. */
  public static SimpleModule currencyCodeModule() {
    SimpleModule module = new SimpleModule("CurrencyCodeModule");
    module.addSerializer(
        Currency.class,
        new JsonSerializer<Currency>() {
          @Override
          public void serialize(Currency value, JsonGenerator gen, SerializerProvider serializers)
              throws IOException {
            gen.writeString(value.getCurrencyCode());
          }
        });
    module.addDeserializer(
        Currency.class,
        new JsonDeserializer<Currency>() {
          @Override
          public Currency deserialize(JsonParser p, DeserializationContext ctxt)
              throws IOException {
            return Currency.getInstance(p.getValueAsString());
          }
        });
    return module;
  }
}
