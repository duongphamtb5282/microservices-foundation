package com.pacific.core.messaging.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for security operations including encryption, API key validation, and service-to-service
 * authentication.
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 16;

  private final SecurityProperties securityProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  /** Encrypt sensitive data using AES-GCM. */
  public String encrypt(String data) {
    try {
      if (data == null || data.isEmpty()) {
        return data;
      }

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      SecretKey key = getEncryptionKey();

      // Generate random IV
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

      byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

      // Combine IV and encrypted data
      byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
      System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
      System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);

      return Base64.getEncoder().encodeToString(encryptedWithIv);

    } catch (Exception e) {
      log.error("Failed to encrypt data", e);
      throw new SecurityException("Encryption failed", e);
    }
  }

  /** Decrypt sensitive data using AES-GCM. */
  public String decrypt(String encryptedData) {
    try {
      if (encryptedData == null || encryptedData.isEmpty()) {
        return encryptedData;
      }

      byte[] decodedData = Base64.getDecoder().decode(encryptedData);

      if (decodedData.length < GCM_IV_LENGTH) {
        throw new IllegalArgumentException("Invalid encrypted data length");
      }

      // Extract IV and encrypted data
      byte[] iv = new byte[GCM_IV_LENGTH];
      byte[] encrypted = new byte[decodedData.length - GCM_IV_LENGTH];

      System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
      System.arraycopy(decodedData, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      SecretKey key = getEncryptionKey();

      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

      byte[] decryptedData = cipher.doFinal(encrypted);
      return new String(decryptedData, StandardCharsets.UTF_8);

    } catch (Exception e) {
      log.error("Failed to decrypt data", e);
      throw new SecurityException("Decryption failed", e);
    }
  }

  /** Validate API key for service-to-service authentication. */
  public boolean validateApiKey(String apiKey) {
    if (apiKey == null || apiKey.isEmpty()) {
      return false;
    }

    return securityProperties.getValidApiKeys().contains(apiKey);
  }

  /** Generate a new API key for service registration. */
  public String generateApiKey(String serviceName) {
    try {
      byte[] keyBytes = new byte[32]; // 256-bit key
      secureRandom.nextBytes(keyBytes);

      String apiKey = Base64.getEncoder().encodeToString(keyBytes);

      log.info("Generated API key for service: {}", serviceName);
      return apiKey;

    } catch (Exception e) {
      log.error("Failed to generate API key for service: {}", serviceName, e);
      throw new SecurityException("API key generation failed", e);
    }
  }

  /** Validate service-to-service authentication. */
  public boolean validateServiceAuthentication(String serviceName, String apiKey) {
    if (!validateApiKey(apiKey)) {
      log.warn("Invalid API key for service: {}", serviceName);
      return false;
    }

    // Check service-specific permissions
    return securityProperties.getServicePermissions().getOrDefault(serviceName, false);
  }

  /** Get current authenticated user ID from security context. */
  public Optional<String> getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated()) {
      // Try to extract user ID from JWT token or other authentication
      return Optional.of(authentication.getName());
    }

    return Optional.empty();
  }

  /**
   * Get the security properties.
   *
   * @return the security properties
   */
  public SecurityProperties getSecurityProperties() {
    return securityProperties;
  }

  /**
   * Get the valid API keys from security properties.
   *
   * @return the set of valid API keys
   */
  public Set<String> getValidApiKeys() {
    return securityProperties.getValidApiKeys();
  }

  /** Check if current user has permission for operation. */
  public boolean hasPermission(String permission) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    // Check authorities/roles
    return authentication.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals(permission));
  }

  /** Hash sensitive data for storage (one-way). */
  public String hashData(String data) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (Exception e) {
      log.error("Failed to hash data", e);
      throw new SecurityException("Data hashing failed", e);
    }
  }

  /** Generate secure random token. */
  public String generateSecureToken() {
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    return Base64.getEncoder().encodeToString(tokenBytes);
  }

  /** Get encryption key from configuration or generate new one. */
  private SecretKey getEncryptionKey() {
    String keyString = securityProperties.getEncryptionKey();

    if (keyString == null || keyString.isEmpty()) {
      log.warn("No encryption key configured, generating temporary key");
      // In production, this should be loaded from secure vault
      return generateEncryptionKey();
    }

    byte[] keyBytes = Base64.getDecoder().decode(keyString);
    return new SecretKeySpec(keyBytes, ALGORITHM);
  }

  /** Generate a new encryption key (for development/testing only). */
  private SecretKey generateEncryptionKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(256); // AES-256
      return keyGenerator.generateKey();
    } catch (Exception e) {
      log.error("Failed to generate encryption key", e);
      throw new SecurityException("Key generation failed", e);
    }
  }

  /** Custom security exception. */
  public static class SecurityException extends RuntimeException {
    public SecurityException(String message) {
      super(message);
    }

    public SecurityException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
