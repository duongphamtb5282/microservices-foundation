package com.pacific.core.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an audit event that captures changes to entities. This provides a comprehensive audit
 * trail for all entity operations.
 */
public record AuditEvent(
    UUID eventId,
    String entityType,
    UUID entityId,
    AuditAction action,
    String userId,
    Instant timestamp,
    String details,
    String oldValue,
    String newValue,
    String ipAddress,
    String userAgent) {

  /**
   * Creates a new audit event.
   *
   * @param entityType the type of entity being audited
   * @param entityId the ID of the entity being audited
   * @param action the audit action performed
   * @param userId the user who performed the action
   * @param details additional details about the audit event
   * @param oldValue the previous state (for updates)
   * @param newValue the new state
   * @param ipAddress the IP address of the user
   * @param userAgent the user agent string
   * @return a new AuditEvent
   */
  public static AuditEvent create(
      String entityType,
      UUID entityId,
      AuditAction action,
      String userId,
      String details,
      String oldValue,
      String newValue,
      String ipAddress,
      String userAgent) {
    return new AuditEvent(
        UUID.randomUUID(),
        entityType,
        entityId,
        action,
        userId,
        Instant.now(),
        details,
        oldValue,
        newValue,
        ipAddress,
        userAgent);
  }

  /**
   * Creates a simple audit event without old/new values.
   *
   * @param entityType the type of entity being audited
   * @param entityId the ID of the entity being audited
   * @param action the audit action performed
   * @param userId the user who performed the action
   * @param details additional details about the audit event
   * @return a new AuditEvent
   */
  public static AuditEvent create(
      String entityType, UUID entityId, AuditAction action, String userId, String details) {
    return create(entityType, entityId, action, userId, details, null, null, null, null);
  }
}
