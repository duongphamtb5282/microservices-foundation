package com.pacific.auth.common.exception;

/** Exception thrown when a refresh token revocation fails (e.g. Redis blacklist write error). */
public class TokenRevocationException extends RuntimeException {

  public TokenRevocationException(String message) {
    super(message);
  }

  public TokenRevocationException(String message, Throwable cause) {
    super(message, cause);
  }
}
