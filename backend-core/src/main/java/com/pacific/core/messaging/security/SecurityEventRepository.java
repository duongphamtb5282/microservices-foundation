package com.pacific.core.messaging.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Repository interface for security events and scan results. */
public interface SecurityEventRepository {

  /** Save a security event. */
  void saveSecurityEvent(SecurityEvent event);

  /** Get security events by type. */
  List<SecurityEvent> getSecurityEventsByType(String eventType);

  /** Get security events by user ID. */
  List<SecurityEvent> getSecurityEventsByUserId(String userId);

  /** Get security events in time range. */
  List<SecurityEvent> getSecurityEventsInTimeRange(LocalDateTime startTime, LocalDateTime endTime);

  /** Get security events by severity. */
  List<SecurityEvent> getSecurityEventsBySeverity(SecurityEvent.Severity severity);

  /** Get recent security events. */
  List<SecurityEvent> getRecentSecurityEvents(int limit);

  /** Get security events by IP address. */
  List<SecurityEvent> getSecurityEventsByIpAddress(String ipAddress);

  /** Save security scan result. */
  void saveSecurityScanResult(SecurityScannerService.SecurityScanResult scanResult);

  /** Get recent security scans. */
  List<SecurityScannerService.SecurityScanResult> getRecentSecurityScans(int limit);

  /** Get security event statistics. */
  SecurityEventStatistics getSecurityEventStatistics();

  /** Security event statistics. */
  class SecurityEventStatistics {
    private final long totalEvents;
    private final Map<SecurityEvent.Severity, Long> eventsBySeverity;
    private final Map<String, Long> eventsByType;
    private final Map<String, Long> eventsBySource;

    public SecurityEventStatistics(
        long totalEvents,
        Map<SecurityEvent.Severity, Long> eventsBySeverity,
        Map<String, Long> eventsByType,
        Map<String, Long> eventsBySource) {
      this.totalEvents = totalEvents;
      this.eventsBySeverity = eventsBySeverity;
      this.eventsByType = eventsByType;
      this.eventsBySource = eventsBySource;
    }

    public long getTotalEvents() {
      return totalEvents;
    }

    public Map<SecurityEvent.Severity, Long> getEventsBySeverity() {
      return eventsBySeverity;
    }

    public Map<String, Long> getEventsByType() {
      return eventsByType;
    }

    public Map<String, Long> getEventsBySource() {
      return eventsBySource;
    }
  }
}
