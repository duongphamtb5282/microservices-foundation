package com.pacific.core.cache;

/**
 * Thrown when a cache tier fails to load or store a value. Typed so cache clients can distinguish
 * cache infrastructure failures from application errors instead of catching bare RuntimeException.
 */
public class CacheAccessException extends RuntimeException {

  public CacheAccessException(String message) {
    super(message);
  }

  public CacheAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
