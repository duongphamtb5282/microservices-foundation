package com.pacific.core;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base properties for backend-core. Contains only common properties shared across all
 * microservices. Service-specific properties should be defined in each service.
 */
@Data
@ConfigurationProperties
public class BackendCoreProperties {

  private final Cache cache = new Cache();
  private final Security security = new Security();
  private final Database database = new Database();
  private final Mongo mongo = new Mongo();
  private final Monitoring monitoring = new Monitoring();

  @Data
  public static class Cache {

    private boolean enabled = true;
    private String type = "redis";

    // L1 Cache (Caffeine)
    private final L1Cache l1Cache = new L1Cache();

    // L2 Cache (Redis)
    private final L2Cache l2Cache = new L2Cache();

    @Data
    public static class L1Cache {

      private boolean enabled = true;
      private int maxSize = 1000;
      private long ttlMinutes = 15;
    }

    @Data
    public static class L2Cache {

      private boolean enabled = true;
      private long ttlMinutes = 30;
    }
  }

  @Data
  public static class Security {

    private boolean enabled = true;
    private final Cors cors = new Cors();
    private String defaultAuthMode = "jwt";
    private boolean enableCors = true;
    private boolean enableCsrf = false;

    @Data
    public static class Cors {

      /**
       * Allowed origins for CORS requests. WARNING: Never use "*" with allowCredentials=true in
       * production. Specify exact origins instead.
       */
      private String allowedOrigins = "http://localhost:3000,http://localhost:4200";

      private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS,PATCH";
      private String allowedHeaders = "*";
      private String exposedHeaders = "Authorization,Content-Type";

      /**
       * Allow credentials (cookies, authorization headers) in CORS requests. If true,
       * allowedOrigins MUST NOT be "*" for security reasons.
       */
      private boolean allowCredentials = true;

      private long maxAge = 3600; // In seconds
    }
  }

  @Data
  public static class Database {

    private boolean enabled = true;
    private String defaultPoolType = "hikari";
    private int defaultMinIdle = 2;
    private int defaultMaxPoolSize = 10;
    private Duration defaultConnectionTimeout = Duration.ofSeconds(20);
    private Duration defaultIdleTimeout = Duration.ofMinutes(5);
    private Duration defaultMaxLifetime = Duration.ofMinutes(10);
  }

  @Data
  public static class Mongo {

    private boolean enabled = true;
    private int defaultPoolMaxSize = 20;
    private int defaultPoolMinSize = 0;
    private Duration defaultConnectionTimeout = Duration.ofSeconds(10);
    private Duration defaultSocketTimeout = Duration.ofSeconds(30);
    private boolean defaultRetryReads = true;
    private boolean defaultRetryWrites = true;
  }

  @Data
  public static class Monitoring {

    private boolean enabled = true;
    private boolean enableHealthChecks = true;
    private boolean enableMetrics = true;
    private boolean enableLogging = true;
    private String logLevel = "INFO";
  }
}
