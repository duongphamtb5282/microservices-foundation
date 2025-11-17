package com.pacific.auth.modules.authentication.service;

import com.pacific.auth.modules.authentication.dto.request.RefreshTokenRequestDto;
import com.pacific.auth.modules.authentication.dto.response.AuthenticationResponseDto;
import com.pacific.auth.modules.authentication.security.jwt.common.JwtValidationResult;
import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakTokenValidationService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Stateless refresh token service using JWT tokens without database storage. */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class StatelessRefreshTokenService {

  private final JwtService jwtService;
  private final KeycloakTokenValidationService tokenValidationService;
  private final RedisTemplate<String, String> redisTemplate;

  @Value("${api-java.security.authentication.jwt.refresh-token-ttl:PT168H}")
  private Duration refreshTokenTtl;

  @Value("${api-java.security.authentication.jwt.max-refresh-tokens-per-user:5}")
  private int maxRefreshTokensPerUser;

  /** Generate a stateless refresh token */
  public String generateStatelessRefreshToken(String username) {
    log.debug("Generating stateless refresh token for user: {}", username);

    // Check token limit (optional - can be removed for true stateless)
    if (hasTooManyActiveTokens(username)) {
      log.warn("User {} has too many active refresh tokens", username);
      // Optionally revoke oldest tokens or deny new token
    }

    // Generate JWT refresh token
    String refreshToken = jwtService.generateRefreshToken(username);

    // Track active tokens for user (optional)
    trackActiveToken(username, refreshToken);

    return refreshToken;
  }

  /** Validate stateless refresh token and generate new access token */
  public AuthenticationResponseDto refreshToken(RefreshTokenRequestDto request) {
    String refreshToken = request.refreshToken();
    log.debug("Validating refresh token");

    try {
      // Validate the refresh token
      JwtValidationResult validation = tokenValidationService.validateToken(refreshToken);

      if (!validation.isValid()) {
        log.warn("Invalid refresh token: {}", validation.getErrorMessage());
        throw new RuntimeException("Invalid refresh token: " + validation.getErrorMessage());
      }

      // Check if token is blacklisted
      if (isTokenBlacklisted(refreshToken)) {
        log.warn("Refresh token has been revoked");
        throw new RuntimeException("Refresh token has been revoked");
      }

      String username = validation.getUsername();
      log.info("Refresh token validated for user: {}", username);

      // Generate new access token
      String newAccessToken = jwtService.generateAccessToken(username);

      // Generate new refresh token (token rotation)
      String newRefreshToken = generateStatelessRefreshToken(username);

      // Revoke old refresh token
      revokeRefreshToken(refreshToken);

      log.info("Successfully refreshed tokens for user: {}", username);

      return AuthenticationResponseDto.builder()
          .accessToken(newAccessToken)
          .refreshToken(newRefreshToken)
          .tokenType("Bearer")
          .username(username)
          .build();

    } catch (Exception e) {
      log.error("Error refreshing token: {}", e.getMessage());
      throw new RuntimeException("Failed to refresh token: " + e.getMessage());
    }
  }

  /** Revoke refresh token by adding to blacklist */
  public void revokeRefreshToken(String token) {
    try {
      // Extract token ID from JWT (if available)
      String tokenId = extractTokenId(token);
      if (tokenId != null) {
        // Add to Redis blacklist with TTL
        String blacklistKey = "blacklisted_token:" + tokenId;
        redisTemplate.opsForValue().set(blacklistKey, "revoked", refreshTokenTtl);
        log.debug("Token {} added to blacklist", tokenId);
      }

      // Also add full token hash to blacklist for additional security
      String tokenHash = String.valueOf(token.hashCode());
      String tokenBlacklistKey = "blacklisted_token_hash:" + tokenHash;
      redisTemplate.opsForValue().set(tokenBlacklistKey, "revoked", refreshTokenTtl);

    } catch (Exception e) {
      log.warn("Failed to revoke token: {}", e.getMessage());
    }
  }

  /** Revoke all refresh tokens for a user */
  public void revokeAllUserTokens(String username) {
    try {
      // Add user to global blacklist
      String userBlacklistKey = "user_blacklisted:" + username;
      redisTemplate.opsForValue().set(userBlacklistKey, "revoked", refreshTokenTtl);

      // Clear active tokens for user
      String userTokensKey = "user_active_tokens:" + username;
      redisTemplate.delete(userTokensKey);

      log.info("Revoked all tokens for user: {}", username);
    } catch (Exception e) {
      log.warn("Failed to revoke all tokens for user {}: {}", username, e.getMessage());
    }
  }

  /** Check if token is blacklisted */
  private boolean isTokenBlacklisted(String token) {
    try {
      // Check by token ID
      String tokenId = extractTokenId(token);
      if (tokenId != null) {
        String blacklistKey = "blacklisted_token:" + tokenId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
          return true;
        }
      }

      // Check by token hash
      String tokenHash = String.valueOf(token.hashCode());
      String tokenBlacklistKey = "blacklisted_token_hash:" + tokenHash;
      return Boolean.TRUE.equals(redisTemplate.hasKey(tokenBlacklistKey));

    } catch (Exception e) {
      log.warn("Error checking token blacklist: {}", e.getMessage());
      return true; // If we can't check, consider it blacklisted for security
    }
  }

  /** Track active token for user (optional) */
  private void trackActiveToken(String username, String token) {
    try {
      String userTokensKey = "user_active_tokens:" + username;
      String tokenId = extractTokenId(token);
      if (tokenId != null) {
        redisTemplate.opsForSet().add(userTokensKey, tokenId);
        redisTemplate.expire(userTokensKey, refreshTokenTtl);
      }
    } catch (Exception e) {
      log.warn("Failed to track active token for user {}: {}", username, e.getMessage());
    }
  }

  /** Check if user has too many active tokens */
  private boolean hasTooManyActiveTokens(String username) {
    try {
      String userTokensKey = "user_active_tokens:" + username;
      Long tokenCount = redisTemplate.opsForSet().size(userTokensKey);
      return tokenCount != null && tokenCount >= maxRefreshTokensPerUser;
    } catch (Exception e) {
      log.warn("Error checking token count for user {}: {}", username, e.getMessage());
      return false;
    }
  }

  /** Extract token ID from JWT (simplified - in real implementation, parse JWT) */
  private String extractTokenId(String token) {
    // This is a simplified implementation
    // In a real application, you would parse the JWT and extract the 'jti' claim
    return UUID.randomUUID().toString();
  }
}
