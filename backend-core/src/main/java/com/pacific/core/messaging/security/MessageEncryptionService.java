package com.pacific.core.messaging.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.pacific.shared.messaging.cqrs.event.DomainEvent;

/**
 * Service for encrypting/decrypting Kafka messages and events. Provides end-to-end encryption for
 * sensitive event data.
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class MessageEncryptionService {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 16;

  private final SecurityService securityService;
  private final ObjectMapper objectMapper;
  private final SecureRandom secureRandom = new SecureRandom();

  // Cache for topic-specific encryption keys
  private final Map<String, SecretKey> topicKeys = new ConcurrentHashMap<>();

  /** Encrypt event data for publishing to Kafka. */
  public String encryptEvent(DomainEvent event, String topic) {
    try {
      // Convert event to JSON
      String eventJson = objectMapper.writeValueAsString(event);

      // Check if event contains sensitive data
      if (containsSensitiveData(event)) {
        log.debug("Encrypting sensitive event data for topic: {}", topic);
        return securityService.encrypt(eventJson);
      } else {
        // For non-sensitive events, just base64 encode for consistency
        return Base64.getEncoder().encodeToString(eventJson.getBytes(StandardCharsets.UTF_8));
      }

    } catch (Exception e) {
      log.error("Failed to encrypt event for topic: {}", topic, e);
      throw new MessageEncryptionException("Event encryption failed", e);
    }
  }

  /** Decrypt event data received from Kafka. */
  @SuppressWarnings("unchecked")
  public <T> T decryptEvent(String encryptedData, Class<T> eventClass, String topic) {
    try {
      String decryptedJson;

      // Check if data is encrypted (contains IV and tag)
      if (isEncrypted(encryptedData)) {
        log.debug("Decrypting sensitive event data from topic: {}", topic);
        decryptedJson = securityService.decrypt(encryptedData);
      } else {
        // Data is base64 encoded, decode it
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        decryptedJson = new String(decodedBytes, StandardCharsets.UTF_8);
      }

      return objectMapper.readValue(decryptedJson, eventClass);

    } catch (Exception e) {
      log.error("Failed to decrypt event from topic: {}", topic, e);
      throw new MessageEncryptionException("Event decryption failed", e);
    }
  }

  /** Encrypt message payload for Kafka. */
  public String encryptMessage(String payload, String topic) {
    try {
      if (payload == null || payload.isEmpty()) {
        return payload;
      }

      // Check if topic requires encryption
      if (isEncryptionRequired(topic)) {
        return securityService.encrypt(payload);
      } else {
        // Return base64 encoded for consistency
        return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
      }

    } catch (Exception e) {
      log.error("Failed to encrypt message for topic: {}", topic, e);
      throw new MessageEncryptionException("Message encryption failed", e);
    }
  }

  /** Decrypt message payload from Kafka. */
  public String decryptMessage(String encryptedPayload, String topic) {
    try {
      if (encryptedPayload == null || encryptedPayload.isEmpty()) {
        return encryptedPayload;
      }

      if (isEncrypted(encryptedPayload)) {
        return securityService.decrypt(encryptedPayload);
      } else {
        // Decode from base64
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedPayload);
        return new String(decodedBytes, StandardCharsets.UTF_8);
      }

    } catch (Exception e) {
      log.error("Failed to decrypt message from topic: {}", topic, e);
      throw new MessageEncryptionException("Message decryption failed", e);
    }
  }

  /** Check if event contains sensitive data that should be encrypted. */
  private boolean containsSensitiveData(DomainEvent event) {
    // Check event type and content for sensitive data
    String eventType = event.getEventType();

    // Always encrypt these event types
    if (eventType.contains("PAYMENT")
        || eventType.contains("USER")
        || eventType.contains("CUSTOMER")) {
      return true;
    }

    // Check for specific sensitive fields in the event JSON
    try {
      String eventJson = objectMapper.writeValueAsString(event);
      return eventJson.contains("email")
          || eventJson.contains("phone")
          || eventJson.contains("address")
          || eventJson.contains("payment")
          || eventJson.contains("credit")
          || eventJson.contains("card");
    } catch (Exception e) {
      log.warn("Failed to check event for sensitive data", e);
      return false;
    }
  }

  /** Check if topic requires encryption. */
  private boolean isEncryptionRequired(String topic) {
    // Configure which topics require encryption
    return topic.contains("payment")
        || topic.contains("user")
        || topic.contains("customer")
        || topic.contains("sensitive");
  }

  /** Check if data string is encrypted (has encryption markers). */
  private boolean isEncrypted(String data) {
    if (data == null || data.isEmpty()) {
      return false;
    }

    // Check if it's valid Base64 (all encrypted data is Base64)
    try {
      byte[] decoded = Base64.getDecoder().decode(data);
      // Check if it looks like encrypted data (contains IV and tag)
      return decoded.length > GCM_IV_LENGTH + GCM_TAG_LENGTH;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /** Generate encryption key for a specific topic. */
  public SecretKey generateTopicKey(String topic) {
    try {
      // Generate key based on topic name for consistency
      String keySeed = "kafka-topic-key:" + topic;
      byte[] keyBytes = securityService.hashData(keySeed).getBytes(StandardCharsets.UTF_8);

      // Use first 32 bytes for AES-256
      byte[] aesKey = new byte[32];
      System.arraycopy(keyBytes, 0, aesKey, 0, Math.min(keyBytes.length, 32));

      return new SecretKeySpec(aesKey, ALGORITHM);

    } catch (Exception e) {
      log.error("Failed to generate topic key for: {}", topic, e);
      throw new MessageEncryptionException("Topic key generation failed", e);
    }
  }

  /** Get or create encryption key for topic. */
  public SecretKey getTopicKey(String topic) {
    return topicKeys.computeIfAbsent(topic, this::generateTopicKey);
  }

  /** Clear encryption keys cache (for key rotation). */
  public void clearKeysCache() {
    topicKeys.clear();
    log.info("Cleared encryption keys cache");
  }

  /** Custom exception for message encryption errors. */
  public static class MessageEncryptionException extends RuntimeException {
    public MessageEncryptionException(String message) {
      super(message);
    }

    public MessageEncryptionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
