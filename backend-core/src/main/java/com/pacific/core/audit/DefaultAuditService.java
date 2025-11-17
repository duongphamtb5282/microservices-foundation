package com.pacific.core.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default implementation of AuditService. This is a simple in-memory implementation that can be
 * replaced with a persistent implementation (e.g., database-backed) as needed.
 */
@Slf4j
@Service
public class DefaultAuditService implements AuditService {

  // In-memory storage for audit events - replace with database persistence for production
  private final ConcurrentHashMap<String, List<AuditEvent>> auditEvents = new ConcurrentHashMap<>();

  @Override
  public void recordAuditEvent(AuditEvent event) {
    log.info(
        "Recording audit event: {} - {} - {} by user {}",
        event.entityType(),
        event.action(),
        event.entityId(),
        event.userId());

    String key = event.entityType() + ":" + event.entityId();
    auditEvents.computeIfAbsent(key, k -> new ArrayList<>()).add(event);

    // Also store by user for user-specific queries
    String userKey = "user:" + event.userId();
    auditEvents.computeIfAbsent(userKey, k -> new ArrayList<>()).add(event);
  }

  @Override
  public void recordAuditEvent(
      String entityType, UUID entityId, AuditAction action, String userId, String details) {
    AuditEvent event = AuditEvent.create(entityType, entityId, action, userId, details);
    recordAuditEvent(event);
  }

  @Override
  public List<AuditEvent> getAuditEventsForEntity(String entityType, UUID entityId) {
    String key = entityType + ":" + entityId;
    return new ArrayList<>(auditEvents.getOrDefault(key, new ArrayList<>()));
  }

  @Override
  public List<AuditEvent> getAuditEventsForUser(String userId) {
    String key = "user:" + userId;
    return new ArrayList<>(auditEvents.getOrDefault(key, new ArrayList<>()));
  }

  @Override
  public List<AuditEvent> getAuditEventsInDateRange(
      java.time.Instant startDate, java.time.Instant endDate) {
    List<AuditEvent> result = new ArrayList<>();
    for (List<AuditEvent> events : auditEvents.values()) {
      for (AuditEvent event : events) {
        if (event.timestamp().isAfter(startDate) && event.timestamp().isBefore(endDate)) {
          result.add(event);
        }
      }
    }
    return result;
  }

  @Override
  public List<AuditEvent> getAuditEventsByAction(AuditAction action) {
    List<AuditEvent> result = new ArrayList<>();
    for (List<AuditEvent> events : auditEvents.values()) {
      for (AuditEvent event : events) {
        if (event.action() == action) {
          result.add(event);
        }
      }
    }
    return result;
  }

  /** Clears all audit events. Useful for testing. */
  public void clearAuditEvents() {
    auditEvents.clear();
    log.info("All audit events cleared");
  }
}
