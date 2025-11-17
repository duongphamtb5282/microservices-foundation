package com.pacific.auth.common.exception;

/** Exception thrown when a JWT token is invalid or malformed. */
public class InvalidTokenException extends RuntimeException {

  public InvalidTokenException(String message) {
    super(message);
  }

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }

  public static InvalidTokenException create() {
    return new InvalidTokenException("Invalid JWT token");
  }

  public static InvalidTokenException withReason(String reason) {
    return new InvalidTokenException(String.format("Invalid JWT token: %s", reason));
  }
}
