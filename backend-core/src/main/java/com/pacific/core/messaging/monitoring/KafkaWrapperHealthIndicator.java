package com.pacific.core.messaging.monitoring;

import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import com.pacific.core.messaging.error.DeadLetterQueue;
import com.pacific.core.messaging.error.DlqStats;

/** Health indicator for Kafka wrapper. Checks Kafka connectivity and DLQ status. */
@Slf4j
@Component("kafkaWrapperHealth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaWrapperHealthIndicator implements HealthIndicator {

  private final KafkaAdmin kafkaAdmin;
  private final DeadLetterQueue deadLetterQueue;

  @Override
  public Health health() {
    try {
      // Check Kafka connectivity
      boolean kafkaAvailable = checkKafkaConnection();

      // Check DLQ stats
      DlqStats dlqStats = deadLetterQueue.getStats();

      if (!kafkaAvailable) {
        return Health.down()
            .withDetail("kafka", "unavailable")
            .withDetail("error", "Cannot connect to Kafka cluster")
            .build();
      }

      Health.Builder builder =
          Health.up()
              .withDetail("kafka", "available")
              .withDetail("dlq_total_messages", dlqStats.getTotalMessages())
              .withDetail("dlq_retry_rate", dlqStats.getRetryRate())
              .withDetail("dlq_reprocessed_successfully", dlqStats.getReprocessedSuccessfully())
              .withDetail("dlq_reprocessed_failed", dlqStats.getReprocessedFailed());

      // Warning if too many DLQ messages
      if (dlqStats.getTotalMessages() > 1000) {
        builder
            .status("WARNING")
            .withDetail("warning", "High number of DLQ messages: " + dlqStats.getTotalMessages());
      }

      // Warning if high retry rate
      if (dlqStats.getRetryRate() > 10.0) {
        builder
            .status("WARNING")
            .withDetail("warning", "High retry rate: " + dlqStats.getRetryRate() + " msg/s");
      }

      return builder.build();

    } catch (Exception e) {
      log.error("Health check failed", e);
      return Health.down().withDetail("error", e.getMessage()).withException(e).build();
    }
  }

  /** Check Kafka cluster connectivity. */
  private boolean checkKafkaConnection() {
    try {
      AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
      DescribeClusterResult result = adminClient.describeCluster();
      result.clusterId().get(5, TimeUnit.SECONDS);
      adminClient.close();
      return true;
    } catch (Exception e) {
      log.warn("Kafka connection check failed: {}", e.getMessage());
      return false;
    }
  }
}
