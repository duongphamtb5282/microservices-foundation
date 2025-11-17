package com.pacific.auth.modules.authentication.security.jwt.custom;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Configuration for custom JWT authentication. Handles JWT encoding, decoding, and service creation
 * for custom app tokens. Includes fallback configuration when JWT is disabled or not properly
 * configured.
 */
@Configuration
@Setter
@Getter
@Slf4j
// @ConfigurationProperties(prefix = "auth-service.security.authentication.jwt")
@ConditionalOnProperty(
    name = "auth-service.security.authentication.jwt.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class CustomJwtConfig {

  @Value(
      "${auth-service.security.authentication.jwt.secret:mySecretKeyThatIsAtLeast256BitsLongForHS256Algorithm}")
  private String secret;

  @Value("${auth-service.security.authentication.jwt.enabled:true}")
  private boolean enabled;

  private Duration ttl;
  private Duration refreshTokenTtl;

  /** Custom JWT Encoder - only active when JWT is enabled */
  @Bean("customJwtEncoder")
  @ConditionalOnProperty(
      name = "auth-service.security.authentication.jwt.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public JwtEncoder customJwtEncoder() {
    log.info("🔐 Configuring custom JWT encoder with HMAC secret");
    // For HMAC, we need to create a JWK from the secret key
    com.nimbusds.jose.jwk.OctetSequenceKey jwk =
        new com.nimbusds.jose.jwk.OctetSequenceKey.Builder(getSecretKey().getEncoded())
            .keyID("hmac-key-id")
            .build();
    return new NimbusJwtEncoder(
        new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk)));
  }

  /** Custom JWT Decoder - only active when JWT is enabled */
  @Bean("customJwtDecoder")
  @ConditionalOnProperty(
      name = "auth-service.security.authentication.jwt.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public JwtDecoder customJwtDecoder() {
    log.info("🔐 Configuring custom JWT decoder with HMAC secret");
    return NimbusJwtDecoder.withSecretKey(getSecretKey()).build();
  }

  /**
   * Fallback JWT Decoder when JWT is disabled Provides a no-op decoder to prevent dependency
   * injection failures
   */
  @Bean("jwtDecoder")
  @ConditionalOnMissingBean(name = "jwtDecoder")
  @ConditionalOnProperty(
      name = "auth-service.security.authentication.jwt.enabled",
      havingValue = "false")
  public JwtDecoder fallbackJwtDecoder() {
    log.warn("⚠️ Using fallback JwtDecoder - JWT authentication is disabled");
    return token -> {
      throw new UnsupportedOperationException("JWT authentication is disabled");
    };
  }

  private SecretKey getSecretKey() {
    try {
      log.info("🔐 Creating HMAC secret key from configured secret");
      // Match the logic in JwtService.getSigningKey() for consistency
      // Check if the secret is already base64 encoded
      try {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        log.info("✅ HMAC secret key created successfully (from base64)");
        return key;
      } catch (Exception e) {
        // If base64 decoding fails, treat as plain text and encode it
        log.info("🔐 Using plain text secret, encoding to HMAC key");
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        log.info("✅ HMAC secret key created successfully (from UTF-8)");
        return key;
      }
    } catch (Exception e) {
      log.error("❌ Failed to create HMAC secret key", e);
      throw new RuntimeException("Failed to create HMAC secret key", e);
    }
  }
}
