package com.pacific.customer.config;

/**
 * Thrown when a startup MongoDB migration fails. Startup must abort so a half-migrated index set
 * never serves traffic — failing loudly is intentional.
 */
public class MongoMigrationException extends RuntimeException {

  public MongoMigrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
