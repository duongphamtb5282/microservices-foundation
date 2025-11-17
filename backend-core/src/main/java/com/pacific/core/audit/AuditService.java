package com.pacific.core.audit;

import java.util.List;
import java.util.UUID;

/** Service interface for audit operations. Provides methods to record and query audit events. */
public interface AuditService {

  /**
   * Records an audit event.
   *
   * @param event the audit event to record
   */
  void recordAuditEvent(AuditEvent event);

  /**
   * Records a simple audit event.
   *
   * @param entityType the type of entity
   * @param entityId the entity ID
   * @param action the audit action
   * @param userId the user performing the action
   * @param details additional details
   */
  void recordAuditEvent(
      String entityType, UUID entityId, AuditAction action, String userId, String details);

  /**
   * Gets audit events for a specific entity.
   *
   * @param entityType the type of entity
   * @param entityId the entity ID
   * @return list of audit events
   */
  List<AuditEvent> getAuditEventsForEntity(String entityType, UUID entityId);

  /**
   * Gets audit events for a specific user.
   *
   * @param userId the user ID
   * @return list of audit events
   */
  List<AuditEvent> getAuditEventsForUser(String userId);

  /**
   * Gets audit events within a date range.
   *
   * @param startDate start date
   * @param endDate end date
   * @return list of audit events
   */
  List<AuditEvent> getAuditEventsInDateRange(
      java.time.Instant startDate, java.time.Instant endDate);

  /**
   * Gets audit events by action type.
   *
   * @param action the audit action
   * @return list of audit events
   */
  List<AuditEvent> getAuditEventsByAction(AuditAction action);
}
