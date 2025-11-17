package com.pacific.auth.common.exception;

/** Exception thrown when a JWT token has expired. */
public class TokenExpiredException extends RuntimeException {

  public TokenExpiredException(String message) {
    super(message);
  }

  public TokenExpiredException(String message, Throwable cause) {
    super(message, cause);
  }

  public static TokenExpiredException create() {
    return new TokenExpiredException("JWT token has expired. Please login again.");
  }

  public static TokenExpiredException withDetails(String tokenType) {
    return new TokenExpiredException(
        String.format("%s token has expired. Please obtain a new one.", tokenType));
  }
}
