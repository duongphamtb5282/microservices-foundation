package com.pacific.core.database.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.event.ConnectionCheckedInEvent;
import com.mongodb.event.ConnectionCheckedOutEvent;
import com.mongodb.event.ConnectionClosedEvent;
import com.mongodb.event.ConnectionCreatedEvent;
import com.mongodb.event.ConnectionPoolListener;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Common MongoDB configuration shared across microservices. Provides a {@link
 * MongoClientSettingsBuilderCustomizer} so that Spring Boot's auto-configuration picks up shared
 * tuning while still allowing per-service overrides.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(MongoClientSettings.class)
@ConditionalOnProperty(prefix = "mongo", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MongoDatabaseProperties.class)
public class MongoConfiguration {

  private final MongoDatabaseProperties properties;

  @Bean
  @ConditionalOnMissingBean(name = "backendCoreMongoClientSettingsCustomizer")
  public MongoClientSettingsBuilderCustomizer backendCoreMongoClientSettingsCustomizer(
      Optional<ConnectionPoolListener> poolListener) {
    return builder -> {
      builder.applyConnectionString(buildConnectionString());
      builder.applyToSocketSettings(socket -> configureSocketSettings(socket, properties));
      builder.applyToConnectionPoolSettings(pool -> configurePoolSettings(pool, properties));
      builder.applyToClusterSettings(
          cluster ->
              cluster.serverSelectionTimeout(
                  properties.getServerSelectionTimeout().toMillis(), TimeUnit.MILLISECONDS));
      builder.retryReads(properties.isRetryReads());
      builder.retryWrites(properties.isRetryWrites());
      builder.readPreference(resolveReadPreference(properties.getReadPreference()));
      builder.writeConcern(resolveWriteConcern(properties.getWriteConcern()));

      poolListener.ifPresent(
          listener -> builder.applyToConnectionPoolSettings(s -> s.addConnectionPoolListener(listener)));
    };
  }

  private static void configureSocketSettings(
      SocketSettings.Builder builder, MongoDatabaseProperties props) {
    builder
        .connectTimeout((int) props.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .readTimeout((int) props.getSocketTimeout().toMillis(), TimeUnit.MILLISECONDS);
  }

  private static void configurePoolSettings(
      ConnectionPoolSettings.Builder builder, MongoDatabaseProperties props) {
    builder
        .minSize(props.getPool().getMinSize())
        .maxSize(props.getPool().getMaxSize())
        .maxWaitTime(props.getPool().getMaxWaitTime().toMillis(), TimeUnit.MILLISECONDS)
        .maxConnectionLifeTime(
            props.getPool().getMaxConnectionLifeTime().toMillis(), TimeUnit.MILLISECONDS)
        .maxConnectionIdleTime(
            props.getPool().getMaxConnectionIdleTime().toMillis(), TimeUnit.MILLISECONDS);
  }

  private ConnectionString buildConnectionString() {
    if (StringUtils.hasText(properties.getUri())) {
      return new ConnectionString(properties.getUri());
    }

    StringBuilder builder = new StringBuilder("mongodb://");
    if (StringUtils.hasText(properties.getUsername())) {
      builder.append(properties.getUsername());
      if (StringUtils.hasText(properties.getPassword())) {
        builder.append(":").append(properties.getPassword());
      }
      builder.append("@");
    }

    builder
        .append(properties.getHost())
        .append(":")
        .append(properties.getPort())
        .append("/")
        .append(properties.getDatabase());

    if (StringUtils.hasText(properties.getUsername())) {
      builder.append("?authSource=").append(properties.getAuthenticationDatabase());
    }

    if (properties.isTlsEnabled()) {
      builder.append(StringUtils.hasText(properties.getUsername()) ? "&" : "?");
      builder.append("tls=true");
    }

    return new ConnectionString(builder.toString());
  }

  private static ReadPreference resolveReadPreference(String configured) {
    if (!StringUtils.hasText(configured)) {
      return ReadPreference.primary();
    }

    try {
      return ReadPreference.valueOf(configured.trim());
    } catch (IllegalArgumentException ex) {
      log.warn("Unknown MongoDB read preference '{}', falling back to primary", configured);
      return ReadPreference.primary();
    }
  }

  private static WriteConcern resolveWriteConcern(String configured) {
    if (!StringUtils.hasText(configured)) {
      return WriteConcern.MAJORITY;
    }

    return switch (configured.trim().toUpperCase()) {
      case "ACKNOWLEDGED", "W1" -> WriteConcern.ACKNOWLEDGED;
      case "W2" -> WriteConcern.W2;
      case "W3" -> WriteConcern.W3;
      case "MAJORITY" -> WriteConcern.MAJORITY;
      case "JOURNALED" -> WriteConcern.JOURNALED;
      default -> {
        log.warn("Unknown MongoDB write concern '{}', defaulting to MAJORITY", configured);
        yield WriteConcern.MAJORITY;
      }
    };
  }

  @Bean
  @ConditionalOnBean(MeterRegistry.class)
  @ConditionalOnMissingBean(name = "backendCoreMongoPoolListener")
  public ConnectionPoolListener backendCoreMongoPoolListener(MeterRegistry meterRegistry) {
    return new MongoConnectionPoolMetricsListener(meterRegistry, properties.getDatabase());
  }

  @PostConstruct
  public void logMongoConfiguration() {
    log.info("🔧 MongoDB configuration enabled: {}", properties.isEnabled());
    log.info("   - URI: {}", StringUtils.hasText(properties.getUri()) ? properties.getUri() : "constructed");
    log.info(
        "   - Pool size (min/max): {}/{}",
        properties.getPool().getMinSize(),
        properties.getPool().getMaxSize());
    log.info(
        "   - Timeouts (connect/socket): {}/{}",
        properties.getConnectionTimeout(),
        properties.getSocketTimeout());
    log.info("   - Read preference: {}", properties.getReadPreference());
    log.info("   - Write concern: {}", properties.getWriteConcern());
  }

  /**
   * Simple connection pool listener that keeps track of in-use and available connections for metric
   * export.
   */
  static final class MongoConnectionPoolMetricsListener implements ConnectionPoolListener {

    private final AtomicInteger availableConnections = new AtomicInteger();
    private final AtomicInteger inUseConnections = new AtomicInteger();

    MongoConnectionPoolMetricsListener(MeterRegistry meterRegistry, String database) {
      Gauge.builder("mongo.pool.connections.available", availableConnections, AtomicInteger::get)
          .description("MongoDB available connections")
          .tag("database", database)
          .register(meterRegistry);

      Gauge.builder("mongo.pool.connections.in-use", inUseConnections, AtomicInteger::get)
          .description("MongoDB in-use connections")
          .tag("database", database)
          .register(meterRegistry);
    }

    @Override
    public void connectionCreated(ConnectionCreatedEvent event) {
      availableConnections.incrementAndGet();
    }

    @Override
    public void connectionClosed(ConnectionClosedEvent event) {
      decrementSafely(availableConnections);
    }

    @Override
    public void connectionCheckedOut(ConnectionCheckedOutEvent event) {
      inUseConnections.incrementAndGet();
      decrementSafely(availableConnections);
    }

    @Override
    public void connectionCheckedIn(ConnectionCheckedInEvent event) {
      decrementSafely(inUseConnections);
      availableConnections.incrementAndGet();
    }

    private static void decrementSafely(AtomicInteger counter) {
      counter.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }
  }
}


