package com.pacific.auth.common.exception;

/** Thrown when auth-service cache configuration is incomplete (e.g. missing service prefix). */
public class CacheConfigurationException extends RuntimeException {

  public CacheConfigurationException(String message) {
    super(message);
  }
}
