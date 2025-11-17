package com.pacific.core.messaging.security;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/** Security event for logging and monitoring security-related activities. */
@Data
@Builder
public class SecurityEvent {

  private String eventId;
  private String eventType;
  private String description;
  private Severity severity;
  private String source;
  private LocalDateTime timestamp;
  private String userId;
  private String ipAddress;
  private String userAgent;
  private Map<String, Object> details;

  public enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  /** Create security event for authentication failure. */
  public static SecurityEvent authFailure(String userId, String ipAddress, String reason) {
    return SecurityEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("AUTHENTICATION_FAILED")
        .description("Authentication failure: " + reason)
        .severity(Severity.HIGH)
        .source("AuthenticationService")
        .timestamp(LocalDateTime.now())
        .userId(userId)
        .ipAddress(ipAddress)
        .details(Map.of("reason", reason, "timestamp", LocalDateTime.now().toString()))
        .build();
  }

  /** Create security event for API key validation failure. */
  public static SecurityEvent apiKeyFailure(String apiKeyHash, String ipAddress, String endpoint) {
    return SecurityEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("API_KEY_VALIDATION_FAILED")
        .description("Invalid API key used for endpoint: " + endpoint)
        .severity(Severity.HIGH)
        .source("ApiKeyAuthenticationFilter")
        .timestamp(LocalDateTime.now())
        .ipAddress(ipAddress)
        .details(
            Map.of(
                "apiKeyHash", apiKeyHash,
                "endpoint", endpoint,
                "timestamp", LocalDateTime.now().toString()))
        .build();
  }

  /** Create security event for suspicious activity. */
  public static SecurityEvent suspiciousActivity(
      String description, String ipAddress, Map<String, Object> details) {
    return SecurityEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("SUSPICIOUS_ACTIVITY")
        .description(description)
        .severity(Severity.CRITICAL)
        .source("SecurityScannerService")
        .timestamp(LocalDateTime.now())
        .ipAddress(ipAddress)
        .details(details)
        .build();
  }

  /** Create security event for data access. */
  public static SecurityEvent dataAccess(
      String userId, String resourceId, String action, String ipAddress) {
    return SecurityEvent.builder()
        .eventId(java.util.UUID.randomUUID().toString())
        .eventType("DATA_ACCESS")
        .description("Data access: " + action + " on resource: " + resourceId)
        .severity(Severity.MEDIUM)
        .source("DataAccessService")
        .timestamp(LocalDateTime.now())
        .userId(userId)
        .ipAddress(ipAddress)
        .details(
            Map.of(
                "resourceId", resourceId,
                "action", action,
                "timestamp", LocalDateTime.now().toString()))
        .build();
  }
}
