package com.pacific.auth.common.exception;

/** Thrown when the JWT decoder/HMAC key cannot be configured at startup. */
public class JwtConfigurationException extends RuntimeException {

  public JwtConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
