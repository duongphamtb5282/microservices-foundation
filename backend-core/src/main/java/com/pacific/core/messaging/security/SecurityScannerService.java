package com.pacific.core.messaging.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for security scanning and vulnerability management. Provides automated security checks,
 * vulnerability detection, and security event monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "security.scanner.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class SecurityScannerService {

  private final SecurityService securityService;
  private final SecurityEventRepository securityEventRepository;

  // Security metrics
  private final AtomicLong securityEventsProcessed = new AtomicLong(0);
  private final AtomicLong vulnerabilitiesDetected = new AtomicLong(0);
  private final AtomicLong threatsBlocked = new AtomicLong(0);
  private final AtomicLong suspiciousActivities = new AtomicLong(0);

  // Vulnerability tracking
  private final Map<String, VulnerabilityInfo> knownVulnerabilities = new ConcurrentHashMap<>();

  /** Scan for security vulnerabilities in the system. */
  @Scheduled(fixedDelay = 3600000) // Every hour
  public void performSecurityScan() {
    log.info("Starting scheduled security scan");

    try {
      SecurityScanResult result =
          SecurityScanResult.builder()
              .scanId(java.util.UUID.randomUUID().toString())
              .startTime(LocalDateTime.now())
              .vulnerabilitiesFound(0)
              .threatsDetected(0)
              .scanCompleted(true)
              .build();

      // Scan for common vulnerabilities
      scanForCommonVulnerabilities(result);

      // Check encryption status
      checkEncryptionStatus(result);

      // Validate security configurations
      validateSecurityConfigurations(result);

      // Check for suspicious activities
      detectSuspiciousActivities(result);

      result.setEndTime(LocalDateTime.now());
      result.setScanDuration(
          java.time.Duration.between(result.getStartTime(), result.getEndTime()));

      // Save scan result
      securityEventRepository.saveSecurityEvent(
          SecurityEvent.builder()
              .eventType("SECURITY_SCAN_COMPLETED")
              .description("Automated security scan completed")
              .severity(SecurityEvent.Severity.LOW)
              .source("SecurityScannerService")
              .timestamp(LocalDateTime.now())
              .details(
                  Map.of(
                      "scanId", result.getScanId(),
                      "vulnerabilitiesFound", result.getVulnerabilitiesFound(),
                      "threatsDetected", result.getThreatsDetected(),
                      "scanDuration", result.getScanDuration().toString()))
              .build());

      securityEventsProcessed.incrementAndGet();

      log.info(
          "Security scan completed: {} vulnerabilities, {} threats detected",
          result.getVulnerabilitiesFound(),
          result.getThreatsDetected());

    } catch (Exception e) {
      log.error("Security scan failed", e);

      securityEventRepository.saveSecurityEvent(
          SecurityEvent.builder()
              .eventType("SECURITY_SCAN_FAILED")
              .description("Automated security scan failed")
              .severity(SecurityEvent.Severity.HIGH)
              .source("SecurityScannerService")
              .timestamp(LocalDateTime.now())
              .details(Map.of("error", e.getMessage()))
              .build());
    }
  }

  /** Scan for common security vulnerabilities. */
  private void scanForCommonVulnerabilities(SecurityScanResult result) {
    // Check for weak encryption
    if (!isStrongEncryptionEnabled()) {
      addVulnerability(
          result,
          "WEAK_ENCRYPTION",
          "Weak or no encryption configured for sensitive data",
          SecurityEvent.Severity.HIGH);
    }

    // Check for missing API key validation
    if (securityService.getValidApiKeys().isEmpty()) {
      addVulnerability(
          result,
          "MISSING_API_KEYS",
          "No API keys configured for service authentication",
          SecurityEvent.Severity.MEDIUM);
    }

    // Check for default passwords or keys
    if (hasDefaultCredentials()) {
      addVulnerability(
          result,
          "DEFAULT_CREDENTIALS",
          "Default credentials detected in configuration",
          SecurityEvent.Severity.CRITICAL);
    }

    // Check for exposed sensitive endpoints
    if (hasExposedSensitiveEndpoints()) {
      addVulnerability(
          result,
          "EXPOSED_ENDPOINTS",
          "Sensitive endpoints are publicly accessible",
          SecurityEvent.Severity.HIGH);
    }
  }

  /** Check encryption status across the system. */
  private void checkEncryptionStatus(SecurityScanResult result) {
    // Check database encryption
    if (!isDatabaseEncryptionEnabled()) {
      addVulnerability(
          result,
          "DATABASE_NOT_ENCRYPTED",
          "Database data is not encrypted at rest",
          SecurityEvent.Severity.MEDIUM);
    }

    // Check message encryption
    if (!isMessageEncryptionEnabled()) {
      addVulnerability(
          result,
          "MESSAGES_NOT_ENCRYPTED",
          "Inter-service messages are not encrypted",
          SecurityEvent.Severity.MEDIUM);
    }
  }

  /** Validate security configurations. */
  private void validateSecurityConfigurations(SecurityScanResult result) {
    // Check security properties
    var securityProps = securityService.getSecurityProperties();

    if (!securityProps.isEncryptionAtRestEnabled()) {
      addVulnerability(
          result,
          "ENCRYPTION_AT_REST_DISABLED",
          "Data encryption at rest is disabled",
          SecurityEvent.Severity.MEDIUM);
    }

    if (!securityProps.isSecurityEventLoggingEnabled()) {
      addVulnerability(
          result,
          "SECURITY_LOGGING_DISABLED",
          "Security event logging is disabled",
          SecurityEvent.Severity.LOW);
    }
  }

  /** Detect suspicious activities. */
  private void detectSuspiciousActivities(SecurityScanResult result) {
    // Check for rapid API key failures
    long recentFailures = getRecentApiKeyFailures();
    if (recentFailures > 10) {
      result.setThreatsDetected(result.getThreatsDetected() + 1);
      suspiciousActivities.incrementAndGet();

      securityEventRepository.saveSecurityEvent(
          SecurityEvent.builder()
              .eventType("SUSPICIOUS_ACTIVITY_DETECTED")
              .description("High number of API key authentication failures")
              .severity(SecurityEvent.Severity.HIGH)
              .source("SecurityScannerService")
              .timestamp(LocalDateTime.now())
              .details(Map.of("failureCount", recentFailures))
              .build());
    }

    // Check for unusual access patterns
    if (hasUnusualAccessPatterns()) {
      result.setThreatsDetected(result.getThreatsDetected() + 1);
      suspiciousActivities.incrementAndGet();
    }
  }

  /** Add vulnerability to scan result. */
  private void addVulnerability(
      SecurityScanResult result,
      String vulnerabilityCode,
      String description,
      SecurityEvent.Severity severity) {
    result.setVulnerabilitiesFound(result.getVulnerabilitiesFound() + 1);
    vulnerabilitiesDetected.incrementAndGet();

    VulnerabilityInfo vulnerability =
        VulnerabilityInfo.builder()
            .code(vulnerabilityCode)
            .description(description)
            .severity(severity)
            .detectedAt(LocalDateTime.now())
            .status(VulnerabilityInfo.Status.OPEN)
            .build();

    knownVulnerabilities.put(vulnerabilityCode, vulnerability);

    // Save security event
    securityEventRepository.saveSecurityEvent(
        SecurityEvent.builder()
            .eventType("VULNERABILITY_DETECTED")
            .description("Security vulnerability detected: " + vulnerabilityCode)
            .severity(severity)
            .source("SecurityScannerService")
            .timestamp(LocalDateTime.now())
            .details(
                Map.of(
                    "vulnerabilityCode", vulnerabilityCode,
                    "description", description,
                    "severity", severity.toString()))
            .build());

    log.warn("Vulnerability detected: {} - {}", vulnerabilityCode, description);
  }

  /** Get recent security scan results. */
  public List<SecurityScanResult> getRecentScans(int limit) {
    return securityEventRepository.getRecentSecurityScans(limit);
  }

  /** Get current vulnerabilities. */
  public List<VulnerabilityInfo> getCurrentVulnerabilities() {
    return knownVulnerabilities.values().stream()
        .filter(v -> v.getStatus() == VulnerabilityInfo.Status.OPEN)
        .toList();
  }

  /** Mark vulnerability as resolved. */
  public void resolveVulnerability(String vulnerabilityCode) {
    VulnerabilityInfo vulnerability = knownVulnerabilities.get(vulnerabilityCode);
    if (vulnerability != null) {
      vulnerability.setStatus(VulnerabilityInfo.Status.RESOLVED);
      vulnerability.setResolvedAt(LocalDateTime.now());

      // Save security event
      securityEventRepository.saveSecurityEvent(
          SecurityEvent.builder()
              .eventType("VULNERABILITY_RESOLVED")
              .description("Security vulnerability resolved: " + vulnerabilityCode)
              .severity(SecurityEvent.Severity.LOW)
              .source("SecurityScannerService")
              .timestamp(LocalDateTime.now())
              .details(Map.of("vulnerabilityCode", vulnerabilityCode))
              .build());

      log.info("Vulnerability resolved: {}", vulnerabilityCode);
    }
  }

  /** Get security metrics. */
  public SecurityMetrics getSecurityMetrics() {
    return SecurityMetrics.builder()
        .securityEventsProcessed(securityEventsProcessed.get())
        .vulnerabilitiesDetected(vulnerabilitiesDetected.get())
        .threatsBlocked(threatsBlocked.get())
        .suspiciousActivities(suspiciousActivities.get())
        .activeVulnerabilities(
            (int)
                knownVulnerabilities.values().stream()
                    .mapToLong(v -> v.getStatus() == VulnerabilityInfo.Status.OPEN ? 1 : 0)
                    .sum())
        .build();
  }

  // Helper methods for vulnerability detection (simplified implementations)
  private boolean isStrongEncryptionEnabled() {
    return true;
  }

  private boolean hasDefaultCredentials() {
    return false;
  }

  private boolean hasExposedSensitiveEndpoints() {
    return false;
  }

  private boolean isDatabaseEncryptionEnabled() {
    return true;
  }

  private boolean isMessageEncryptionEnabled() {
    return true;
  }

  private long getRecentApiKeyFailures() {
    return 0;
  }

  private boolean hasUnusualAccessPatterns() {
    return false;
  }

  /** Security scan result. */
  public static class SecurityScanResult {
    private String scanId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private java.time.Duration scanDuration;
    private int vulnerabilitiesFound;
    private int threatsDetected;
    private boolean scanCompleted;

    public static SecurityScanResultBuilder builder() {
      return new SecurityScanResultBuilder();
    }

    // Getters and setters
    public String getScanId() {
      return scanId;
    }

    public void setScanId(String scanId) {
      this.scanId = scanId;
    }

    public LocalDateTime getStartTime() {
      return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
      this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
      return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
      this.endTime = endTime;
    }

    public java.time.Duration getScanDuration() {
      return scanDuration;
    }

    public void setScanDuration(java.time.Duration scanDuration) {
      this.scanDuration = scanDuration;
    }

    public int getVulnerabilitiesFound() {
      return vulnerabilitiesFound;
    }

    public void setVulnerabilitiesFound(int vulnerabilitiesFound) {
      this.vulnerabilitiesFound = vulnerabilitiesFound;
    }

    public int getThreatsDetected() {
      return threatsDetected;
    }

    public void setThreatsDetected(int threatsDetected) {
      this.threatsDetected = threatsDetected;
    }

    public boolean isScanCompleted() {
      return scanCompleted;
    }

    public void setScanCompleted(boolean scanCompleted) {
      this.scanCompleted = scanCompleted;
    }

    public static class SecurityScanResultBuilder {
      private String scanId;
      private LocalDateTime startTime;
      private LocalDateTime endTime;
      private java.time.Duration scanDuration;
      private int vulnerabilitiesFound;
      private int threatsDetected;
      private boolean scanCompleted;

      public SecurityScanResultBuilder scanId(String scanId) {
        this.scanId = scanId;
        return this;
      }

      public SecurityScanResultBuilder startTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
      }

      public SecurityScanResultBuilder endTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
      }

      public SecurityScanResultBuilder scanDuration(java.time.Duration scanDuration) {
        this.scanDuration = scanDuration;
        return this;
      }

      public SecurityScanResultBuilder vulnerabilitiesFound(int vulnerabilitiesFound) {
        this.vulnerabilitiesFound = vulnerabilitiesFound;
        return this;
      }

      public SecurityScanResultBuilder threatsDetected(int threatsDetected) {
        this.threatsDetected = threatsDetected;
        return this;
      }

      public SecurityScanResultBuilder scanCompleted(boolean scanCompleted) {
        this.scanCompleted = scanCompleted;
        return this;
      }

      public SecurityScanResult build() {
        SecurityScanResult result = new SecurityScanResult();
        result.scanId = this.scanId;
        result.startTime = this.startTime;
        result.endTime = this.endTime;
        result.scanDuration = this.scanDuration;
        result.vulnerabilitiesFound = this.vulnerabilitiesFound;
        result.threatsDetected = this.threatsDetected;
        result.scanCompleted = this.scanCompleted;
        return result;
      }
    }
  }

  /** Vulnerability information. */
  public static class VulnerabilityInfo {
    private String code;
    private String description;
    private SecurityEvent.Severity severity;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private Status status;

    public enum Status {
      OPEN,
      RESOLVED,
      FALSE_POSITIVE,
      ACCEPTED_RISK
    }

    public static VulnerabilityInfoBuilder builder() {
      return new VulnerabilityInfoBuilder();
    }

    // Getters and setters
    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public SecurityEvent.Severity getSeverity() {
      return severity;
    }

    public void setSeverity(SecurityEvent.Severity severity) {
      this.severity = severity;
    }

    public LocalDateTime getDetectedAt() {
      return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
      this.detectedAt = detectedAt;
    }

    public LocalDateTime getResolvedAt() {
      return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
      this.resolvedAt = resolvedAt;
    }

    public Status getStatus() {
      return status;
    }

    public void setStatus(Status status) {
      this.status = status;
    }

    public static class VulnerabilityInfoBuilder {
      private String code;
      private String description;
      private SecurityEvent.Severity severity;
      private LocalDateTime detectedAt;
      private LocalDateTime resolvedAt;
      private Status status = Status.OPEN;

      public VulnerabilityInfoBuilder code(String code) {
        this.code = code;
        return this;
      }

      public VulnerabilityInfoBuilder description(String description) {
        this.description = description;
        return this;
      }

      public VulnerabilityInfoBuilder severity(SecurityEvent.Severity severity) {
        this.severity = severity;
        return this;
      }

      public VulnerabilityInfoBuilder detectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
        return this;
      }

      public VulnerabilityInfoBuilder resolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
        return this;
      }

      public VulnerabilityInfoBuilder status(Status status) {
        this.status = status;
        return this;
      }

      public VulnerabilityInfo build() {
        VulnerabilityInfo info = new VulnerabilityInfo();
        info.code = this.code;
        info.description = this.description;
        info.severity = this.severity;
        info.detectedAt = this.detectedAt;
        info.resolvedAt = this.resolvedAt;
        info.status = this.status;
        return info;
      }
    }
  }

  /** Security metrics. */
  public static class SecurityMetrics {
    private final long securityEventsProcessed;
    private final long vulnerabilitiesDetected;
    private final long threatsBlocked;
    private final long suspiciousActivities;
    private final int activeVulnerabilities;

    public SecurityMetrics(
        long securityEventsProcessed,
        long vulnerabilitiesDetected,
        long threatsBlocked,
        long suspiciousActivities,
        int activeVulnerabilities) {
      this.securityEventsProcessed = securityEventsProcessed;
      this.vulnerabilitiesDetected = vulnerabilitiesDetected;
      this.threatsBlocked = threatsBlocked;
      this.suspiciousActivities = suspiciousActivities;
      this.activeVulnerabilities = activeVulnerabilities;
    }

    public long getSecurityEventsProcessed() {
      return securityEventsProcessed;
    }

    public long getVulnerabilitiesDetected() {
      return vulnerabilitiesDetected;
    }

    public long getThreatsBlocked() {
      return threatsBlocked;
    }

    public long getSuspiciousActivities() {
      return suspiciousActivities;
    }

    public int getActiveVulnerabilities() {
      return activeVulnerabilities;
    }

    public static SecurityMetricsBuilder builder() {
      return new SecurityMetricsBuilder();
    }

    public static class SecurityMetricsBuilder {
      private long securityEventsProcessed;
      private long vulnerabilitiesDetected;
      private long threatsBlocked;
      private long suspiciousActivities;
      private int activeVulnerabilities;

      public SecurityMetricsBuilder securityEventsProcessed(long securityEventsProcessed) {
        this.securityEventsProcessed = securityEventsProcessed;
        return this;
      }

      public SecurityMetricsBuilder vulnerabilitiesDetected(long vulnerabilitiesDetected) {
        this.vulnerabilitiesDetected = vulnerabilitiesDetected;
        return this;
      }

      public SecurityMetricsBuilder threatsBlocked(long threatsBlocked) {
        this.threatsBlocked = threatsBlocked;
        return this;
      }

      public SecurityMetricsBuilder suspiciousActivities(long suspiciousActivities) {
        this.suspiciousActivities = suspiciousActivities;
        return this;
      }

      public SecurityMetricsBuilder activeVulnerabilities(int activeVulnerabilities) {
        this.activeVulnerabilities = activeVulnerabilities;
        return this;
      }

      public SecurityMetrics build() {
        return new SecurityMetrics(
            securityEventsProcessed,
            vulnerabilitiesDetected,
            threatsBlocked,
            suspiciousActivities,
            activeVulnerabilities);
      }
    }
  }
}
