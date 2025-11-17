package com.pacific.core.messaging.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for Kafka Wrapper. */
@Data
@ConfigurationProperties(prefix = "kafka")
public class KafkaWrapperProperties {

  /** Enable/disable the Kafka wrapper */
  private boolean enabled = true;

  /** CQRS configuration */
  private Cqrs cqrs = new Cqrs();

  /** Retry configuration */
  private Retry retry = new Retry();

  /** Monitoring configuration */
  private Monitoring monitoring = new Monitoring();

  @Data
  public static class Cqrs {
    /** Enable CQRS pattern */
    private boolean enabled = true;

    /** Command topic name */
    private String commandTopic = "commands";

    /** Event topic name */
    private String eventTopic = "events";

    /** Query topic name */
    private String queryTopic = "queries";

    /** Event store enabled for audit trail */
    private boolean eventStoreEnabled = true;
  }

  @Data
  public static class Retry {
    /** Maximum retry attempts */
    private int maxAttempts = 3;

    /** Initial backoff duration */
    private Duration initialBackoff = Duration.ofSeconds(1);

    /** Maximum backoff duration */
    private Duration maxBackoff = Duration.ofMinutes(5);

    /** Backoff multiplier for exponential backoff */
    private double backoffMultiplier = 2.0;

    /** Jitter factor (0.0-1.0) for randomization */
    private double jitterFactor = 0.1;

    /** Enable Dead Letter Queue */
    private boolean enableDlq = true;

    /** DLQ topic suffix */
    private String dlqTopicSuffix = ".dlq";

    /** Retryable exception class names */
    private List<String> retryableExceptions = new ArrayList<>();

    /** Non-retryable exception class names */
    private List<String> nonRetryableExceptions = new ArrayList<>();
  }

  @Data
  public static class Monitoring {
    /** Enable metrics collection */
    private boolean enabled = true;

    /** Enable distributed tracing */
    private boolean tracingEnabled = true;

    /** Metrics prefix for Micrometer */
    private String metricsPrefix = "kafka.wrapper";
  }
}
