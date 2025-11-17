package com.pacific.core.messaging.security;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.pacific.shared.messaging.cqrs.event.DomainEvent;

/**
 * Wrapper for encrypted domain events. Used to maintain compatibility with existing event consumers
 * while providing encryption.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class EncryptedEventWrapper implements DomainEvent {

  @JsonProperty("eventType")
  private final String eventType;

  @JsonProperty("aggregateId")
  private final String aggregateId;

  @JsonProperty("correlationId")
  private final String correlationId;

  @JsonProperty("encryptedData")
  private final String encryptedData;

  @JsonProperty("timestamp")
  private final Instant timestamp;

  @JsonProperty("encryptionEnabled")
  private final boolean encryptionEnabled;

  @JsonProperty("keyId")
  private final String keyId; // For key rotation support

  @JsonProperty("algorithm")
  private final String algorithm;

  @JsonCreator
  public EncryptedEventWrapper(
      @JsonProperty("eventType") String eventType,
      @JsonProperty("aggregateId") String aggregateId,
      @JsonProperty("correlationId") String correlationId,
      @JsonProperty("encryptedData") String encryptedData,
      @JsonProperty("timestamp") Instant timestamp,
      @JsonProperty("encryptionEnabled") boolean encryptionEnabled,
      @JsonProperty("keyId") String keyId,
      @JsonProperty("algorithm") String algorithm) {
    this.eventType = eventType;
    this.aggregateId = aggregateId;
    this.correlationId = correlationId;
    this.encryptedData = encryptedData;
    this.timestamp = timestamp;
    this.encryptionEnabled = encryptionEnabled;
    this.keyId = keyId;
    this.algorithm = algorithm != null ? algorithm : "AES-GCM";
  }

  @Override
  public String getEventId() {
    return null;
  }

  @Override
  public String getEventType() {
    return "ENCRYPTED_EVENT_WRAPPER";
  }

  @Override
  public String getAggregateId() {
    return aggregateId;
  }

  @Override
  public Instant getOccurredOn() {
    return timestamp;
  }

  @Override
  public String getSource() {
    return null;
  }

  @Override
  public String getCorrelationId() {
    return correlationId;
  }

  @Override
  public Map<String, Object> getMetadata() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("eventType", eventType);
    metadata.put("aggregateId", aggregateId);
    metadata.put("encryptionEnabled", encryptionEnabled);
    metadata.put("keyId", keyId);
    metadata.put("algorithm", algorithm);
    return metadata;
  }

  /** Check if this wrapper contains encrypted data. */
  public boolean hasEncryptedData() {
    return encryptionEnabled && encryptedData != null && !encryptedData.isEmpty();
  }

  /** Get metadata about the encryption. */
  public EventEncryptionMetadata getEncryptionMetadata() {
    return EventEncryptionMetadata.builder()
        .encryptionEnabled(encryptionEnabled)
        .keyId(keyId)
        .algorithm(algorithm)
        .timestamp(timestamp)
        .build();
  }

  /** Metadata about event encryption. */
  public static class EventEncryptionMetadata {
    private final boolean encryptionEnabled;
    private final String keyId;
    private final String algorithm;
    private final Instant timestamp;

    public EventEncryptionMetadata(
        boolean encryptionEnabled, String keyId, String algorithm, Instant timestamp) {
      this.encryptionEnabled = encryptionEnabled;
      this.keyId = keyId;
      this.algorithm = algorithm;
      this.timestamp = timestamp;
    }

    public boolean isEncryptionEnabled() {
      return encryptionEnabled;
    }

    public String getKeyId() {
      return keyId;
    }

    public String getAlgorithm() {
      return algorithm;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public static EventEncryptionMetadataBuilder builder() {
      return new EventEncryptionMetadataBuilder();
    }

    public static class EventEncryptionMetadataBuilder {
      private boolean encryptionEnabled;
      private String keyId;
      private String algorithm;
      private Instant timestamp;

      public EventEncryptionMetadataBuilder encryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
        return this;
      }

      public EventEncryptionMetadataBuilder keyId(String keyId) {
        this.keyId = keyId;
        return this;
      }

      public EventEncryptionMetadataBuilder algorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
      }

      public EventEncryptionMetadataBuilder timestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
      }

      public EventEncryptionMetadata build() {
        return new EventEncryptionMetadata(encryptionEnabled, keyId, algorithm, timestamp);
      }
    }
  }
}
