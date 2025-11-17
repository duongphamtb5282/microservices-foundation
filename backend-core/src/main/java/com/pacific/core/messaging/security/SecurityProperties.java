package com.pacific.core.messaging.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuration properties for security features. */
@Configuration
@ConfigurationProperties(prefix = "backend-core.security")
public class SecurityProperties {

  /** Encryption key for sensitive data (Base64 encoded) */
  private String encryptionKey;

  /** Valid API keys for service-to-service authentication */
  private Set<String> validApiKeys = new HashSet<>();

  /** Service permissions mapping */
  private Map<String, Boolean> servicePermissions = new HashMap<>();

  /** Enable mutual TLS authentication */
  private boolean mtlsEnabled = false;

  /** Path to certificate for mTLS */
  private String certificatePath;

  /** Path to private key for mTLS */
  private String privateKeyPath;

  /** Enable data encryption at rest */
  private boolean encryptionAtRestEnabled = true;

  /** Enable security event logging */
  private boolean securityEventLoggingEnabled = true;

  /** API key expiration time in hours */
  private int apiKeyExpirationHours = 24;

  // Getters and setters
  public String getEncryptionKey() {
    return encryptionKey;
  }

  public void setEncryptionKey(String encryptionKey) {
    this.encryptionKey = encryptionKey;
  }

  public Set<String> getValidApiKeys() {
    return validApiKeys;
  }

  public void setValidApiKeys(Set<String> validApiKeys) {
    this.validApiKeys = validApiKeys;
  }

  public Map<String, Boolean> getServicePermissions() {
    return servicePermissions;
  }

  public void setServicePermissions(Map<String, Boolean> servicePermissions) {
    this.servicePermissions = servicePermissions;
  }

  public boolean isMtlsEnabled() {
    return mtlsEnabled;
  }

  public void setMtlsEnabled(boolean mtlsEnabled) {
    this.mtlsEnabled = mtlsEnabled;
  }

  public String getCertificatePath() {
    return certificatePath;
  }

  public void setCertificatePath(String certificatePath) {
    this.certificatePath = certificatePath;
  }

  public String getPrivateKeyPath() {
    return privateKeyPath;
  }

  public void setPrivateKeyPath(String privateKeyPath) {
    this.privateKeyPath = privateKeyPath;
  }

  public boolean isEncryptionAtRestEnabled() {
    return encryptionAtRestEnabled;
  }

  public void setEncryptionAtRestEnabled(boolean encryptionAtRestEnabled) {
    this.encryptionAtRestEnabled = encryptionAtRestEnabled;
  }

  public boolean isSecurityEventLoggingEnabled() {
    return securityEventLoggingEnabled;
  }

  public void setSecurityEventLoggingEnabled(boolean securityEventLoggingEnabled) {
    this.securityEventLoggingEnabled = securityEventLoggingEnabled;
  }

  public int getApiKeyExpirationHours() {
    return apiKeyExpirationHours;
  }

  public void setApiKeyExpirationHours(int apiKeyExpirationHours) {
    this.apiKeyExpirationHours = apiKeyExpirationHours;
  }
}
