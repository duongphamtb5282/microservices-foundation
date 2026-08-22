package com.pacific.auth.common.exception;

/**
 * Thrown when the SHA-256 digest implementation is unavailable. In practice impossible on a modern
 * JVM, but the failure must be typed rather than a bare IllegalStateException.
 */
public class TokenHashingException extends RuntimeException {

  public TokenHashingException(String message, Throwable cause) {
    super(message, cause);
  }
}
