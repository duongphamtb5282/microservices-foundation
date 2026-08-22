package com.pacific.auth.common.exception;

/**
 * Thrown when a refresh token exchange fails for an unexpected (non-token) reason. The original
 * cause is preserved so diagnostics keep the real failure, while clients only see a sanitized
 * message.
 */
public class RefreshTokenProcessingException extends RuntimeException {

  public RefreshTokenProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
