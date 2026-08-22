package com.pacific.shared.utils;

import com.pacific.shared.exceptions.JsonProcessingRuntimeException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/** JSON utility class for serialization/deserialization */
@Slf4j
public class JsonUtils {

  private static final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JavaTimeModule());

  public static String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      log.error("Error serializing object to JSON", e);
      // Fail loudly instead of silently returning "{}" (9)
      throw new JsonProcessingRuntimeException("Failed to serialize object to JSON", e);
    }
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return objectMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      log.error("Error deserializing JSON to object", e);
      // Fail loudly instead of silently returning null (9)
      throw new JsonProcessingRuntimeException(
          "Failed to deserialize JSON to object of type " + clazz.getName(), e);
    }
  }

  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }
}
