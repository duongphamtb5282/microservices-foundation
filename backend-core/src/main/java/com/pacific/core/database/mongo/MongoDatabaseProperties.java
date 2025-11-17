package com.pacific.core.database.mongo;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MongoDB configuration properties shared across services. Allows consistent tuning of MongoDB
 * clients while keeping service overrides simple.
 */
@Data
@ConfigurationProperties(prefix = "mongo")
public class MongoDatabaseProperties {

  private boolean enabled = true;
  private String uri;
  private String host = "localhost";
  private int port = 27017;
  private String database = "application";
  private String username;
  private String password;
  private String authenticationDatabase = "admin";
  private boolean tlsEnabled = false;
  private Duration connectionTimeout = Duration.ofSeconds(10);
  private Duration socketTimeout = Duration.ofSeconds(30);
  private Duration serverSelectionTimeout = Duration.ofSeconds(30);
  private boolean retryReads = true;
  private boolean retryWrites = true;
  private String readPreference = "primary";
  private String writeConcern = "MAJORITY";
  private final Pool pool = new Pool();

  @Data
  public static class Pool {

    private int minSize = 0;
    private int maxSize = 20;
    private Duration maxWaitTime = Duration.ofSeconds(2);
    private Duration maxConnectionLifeTime = Duration.ZERO;
    private Duration maxConnectionIdleTime = Duration.ofSeconds(60);
  }
}


