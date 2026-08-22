package com.pacific.shared.exceptions;

/** Thrown when JSON serialization or deserialization fails (9). */
public class JsonProcessingRuntimeException extends RuntimeException {

  public JsonProcessingRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
