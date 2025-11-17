package com.pacific.core.cache.serialization;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Type-aware Redis serializer that includes type information in serialized data. This helps with
 * deserialization when the exact type is not known.
 */
@Slf4j
public class TypeAwareRedisSerializer implements RedisSerializer<Object> {

  private final ObjectMapper objectMapper;

  public TypeAwareRedisSerializer() {
    this.objectMapper = new ObjectMapper();

    // Register JavaTimeModule to support Java 8 date/time types
    this.objectMapper.registerModule(new JavaTimeModule());

    // Register Hibernate6Module to handle Hibernate-specific types and lazy-loaded proxies
    Hibernate6Module hibernate6Module = new Hibernate6Module();
    // Configure Hibernate module to force lazy-loaded properties to be serialized as null/empty
    hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
    // Skip lazy-loaded properties that are not initialized
    hibernate6Module.configure(
        Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
    this.objectMapper.registerModule(hibernate6Module);

    // Write dates as ISO strings instead of timestamps
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Fail on empty beans to catch issues early
    this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // Enable default typing to include class information in JSON
    // This allows proper deserialization of objects without knowing the exact type beforehand
    BasicPolymorphicTypeValidator ptv =
        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build();
    this.objectMapper.activateDefaultTyping(
        ptv, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

    log.info("✅ TypeAwareRedisSerializer initialized with default typing enabled");
  }

  @Override
  public byte[] serialize(Object object) throws SerializationException {
    if (object == null) {
      return new byte[0];
    }

    try {
      log.debug("Serializing object of type: {}", object.getClass().getName());
      byte[] bytes = objectMapper.writeValueAsBytes(object);
      log.debug(
          "Successfully serialized object of type: {} ({} bytes)",
          object.getClass().getName(),
          bytes.length);
      return bytes;
    } catch (Exception e) {
      log.error(
          "Error serializing object of type: {}. Error: {}",
          object.getClass().getName(),
          e.getMessage());
      log.error("Failed object details: {}", object, e);
      throw new SerializationException(
          String.format(
              "Error serializing object of type %s: %s",
              object.getClass().getName(), e.getMessage()),
          e);
    }
  }

  @Override
  public Object deserialize(byte[] bytes) throws SerializationException {
    if (bytes == null || bytes.length == 0) {
      return null;
    }

    try {
      log.debug("Deserializing object ({} bytes)", bytes.length);
      Object result = objectMapper.readValue(bytes, Object.class);
      log.debug(
          "Successfully deserialized object of type: {}",
          result != null ? result.getClass().getName() : "null");
      return result;
    } catch (Exception e) {
      log.error("Error deserializing object ({} bytes): {}", bytes.length, e.getMessage());
      throw new SerializationException(
          String.format("Error deserializing object: %s", e.getMessage()), e);
    }
  }
}
