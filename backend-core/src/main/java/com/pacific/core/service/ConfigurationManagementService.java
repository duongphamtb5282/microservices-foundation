package com.pacific.core.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Centralized configuration management service */
@Slf4j
@Service
public class ConfigurationManagementService {

  private final Environment environment;

  @Value("${spring.application.name:backend-core}")
  private String applicationName;

  @Value("${spring.profiles.active:default}")
  private String activeProfile;

  public ConfigurationManagementService(Environment environment) {
    this.environment = environment;
  }

  /** Get all configuration properties */
  public ConfigurationInfo getConfigurationInfo() {
    Map<String, String> properties = new HashMap<>();

    // Get system properties
    Properties systemProps = System.getProperties();
    for (String key : systemProps.stringPropertyNames()) {
      if (key.startsWith("spring.") || key.startsWith("app.")) {
        properties.put(key, systemProps.getProperty(key));
      }
    }

    // Get environment variables
    Map<String, String> envVars = System.getenv();
    for (Map.Entry<String, String> entry : envVars.entrySet()) {
      if (entry.getKey().startsWith("SPRING_") || entry.getKey().startsWith("APP_")) {
        properties.put(entry.getKey(), entry.getValue());
      }
    }

    return ConfigurationInfo.builder()
        .applicationName(applicationName)
        .activeProfile(activeProfile)
        .properties(properties)
        .build();
  }

  /** Get specific configuration value */
  public String getConfigurationValue(String key) {
    return environment.getProperty(key);
  }

  /** Get configuration value with default */
  public String getConfigurationValue(String key, String defaultValue) {
    return environment.getProperty(key, defaultValue);
  }

  /** Check if configuration property exists */
  public boolean hasConfiguration(String key) {
    return environment.containsProperty(key);
  }

  /** Get database configuration */
  public DatabaseConfiguration getDatabaseConfiguration() {
    return DatabaseConfiguration.builder()
        .url(environment.getProperty("spring.datasource.url"))
        .username(environment.getProperty("spring.datasource.username"))
        .driverClassName(environment.getProperty("spring.datasource.driver-class-name"))
        .build();
  }

  /** Get Redis configuration */
  public RedisConfiguration getRedisConfiguration() {
    return RedisConfiguration.builder()
        .host(environment.getProperty("spring.redis.host", "localhost"))
        .port(Integer.parseInt(environment.getProperty("spring.redis.port", "6379")))
        .database(Integer.parseInt(environment.getProperty("spring.redis.database", "0")))
        .build();
  }

  /** Get Kafka configuration */
  public KafkaConfiguration getKafkaConfiguration() {
    return KafkaConfiguration.builder()
        .bootstrapServers(
            environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"))
        .groupId(environment.getProperty("spring.kafka.consumer.group-id", "default-group"))
        .build();
  }

  /** Configuration info DTO */
  @lombok.Data
  @lombok.Builder
  public static class ConfigurationInfo {
    private String applicationName;
    private String activeProfile;
    private Map<String, String> properties;
  }

  /** Database configuration DTO */
  @lombok.Data
  @lombok.Builder
  public static class DatabaseConfiguration {
    private String url;
    private String username;
    private String driverClassName;
  }

  /** Redis configuration DTO */
  @lombok.Data
  @lombok.Builder
  public static class RedisConfiguration {
    private String host;
    private int port;
    private int database;
  }

  /** Kafka configuration DTO */
  @lombok.Data
  @lombok.Builder
  public static class KafkaConfiguration {
    private String bootstrapServers;
    private String groupId;
  }
}
