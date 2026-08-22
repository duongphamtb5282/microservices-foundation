package com.pacific.gateway.exception;

/**
 * Thrown when a Bearer token fails local JWT validation (invalid signature, wrong issuer, expired
 * token, or no signing key available for the token's key id).
 */
public class JwtValidationException extends RuntimeException {

  public JwtValidationException(String message) {
    super(message);
  }

  public JwtValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
