package com.pacific.auth.modules.authentication.controller;

import com.pacific.auth.modules.authentication.dto.request.AuthenticationRequestDto;
import com.pacific.auth.modules.authentication.dto.request.RefreshTokenRequestDto;
import com.pacific.auth.modules.authentication.dto.response.AuthenticationResponseDto;
import com.pacific.auth.modules.authentication.mapper.UserInfoMapper;
import com.pacific.auth.modules.authentication.security.jwt.common.JwtAuthenticationToken;
import com.pacific.auth.modules.authentication.security.jwt.common.JwtValidationResult;
import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakTokenValidationService;
import com.pacific.auth.modules.authentication.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints. Controllers stay thin: mapping/response-shape logic lives in {@link
 * UserInfoMapper}, and unexpected exceptions propagate to {@link
 * com.pacific.auth.common.exception.GlobalExceptionHandler} instead of being caught inline.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user management")
public class AuthenticationController {

  private final AuthenticationService authenticationService;
  private final UserInfoMapper userInfoMapper;

  @Autowired(required = false)
  private KeycloakTokenValidationService tokenValidationService;

  @Operation(summary = "Login", description = "Authenticate user and return JWT tokens")
  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponseDto> login(
      @RequestBody AuthenticationRequestDto request) {
    log.info("🚀 Processing login request for user: {}", request.username());

    log.info(
        "🔍 About to call authenticationService.authenticate for user: {}", request.username());
    AuthenticationResponseDto response = authenticationService.authenticate(request);
    log.info("✅ Login successful for user: {}", request.username());
    log.info("📤 Returning response for user: {}", request.username());
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Refresh token",
      description = "Refresh JWT access token using refresh token")
  @PostMapping("/refresh")
  public ResponseEntity<AuthenticationResponseDto> refreshToken(
      @RequestBody RefreshTokenRequestDto request) {
    log.info("🔄 Processing refresh token request");
    AuthenticationResponseDto response = authenticationService.refreshToken(request);
    log.info("✅ Token refreshed successfully");
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Logout", description = "Logout user and revoke refresh token")
  @PostMapping("/logout")
  public ResponseEntity<Map<String, String>> logout(@RequestBody RefreshTokenRequestDto request) {
    log.info("🚪 Processing logout request");
    authenticationService.logout(request.refreshToken());
    log.info("✅ Logout successful");
    return ResponseEntity.ok(Map.of("message", "Logout successful"));
  }

  // ===== KEYCLOAK INTEGRATION ENDPOINTS =====

  /** Get current user information from Keycloak token */
  @Operation(
      summary = "Get current user info",
      description = "Retrieves user information from the current Keycloak JWT token",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> getCurrentUserInfo(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
      log.warn(
          "Invalid authentication type for user info request: {}",
          authentication == null ? "null" : authentication.getClass().getSimpleName());
      return ResponseEntity.status(401).body(Map.of("error", "Invalid authentication"));
    }

    // For Keycloak tokens, re-validate the token and return the token claims
    if (jwtToken.isKeycloakJwtToken()) {
      if (tokenValidationService == null) {
        return ResponseEntity.status(503)
            .body(Map.of("error", "Keycloak authentication not available"));
      }
      JwtValidationResult validation = tokenValidationService.validateToken(jwtToken.getJwtToken());
      if (!validation.isValid()) {
        log.warn("Invalid Keycloak token for user info request");
        return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
      }

      log.info(
          "Retrieved user info for: {} (Token type: {})",
          validation.getUsername(),
          jwtToken.getTokenType());
      return ResponseEntity.ok(
          userInfoMapper.toKeycloakUserInfo(validation, jwtToken.getTokenType().name()));
    }

    // For custom JWT tokens, extract info directly from authentication
    log.info(
        "Retrieved user info for: {} (Token type: {})",
        authentication.getName(),
        jwtToken.getTokenType());
    return ResponseEntity.ok(
        userInfoMapper.toCustomJwtUserInfo(authentication, jwtToken.getTokenType().name()));
  }

  /** Validate a token and return its information */
  @Operation(
      summary = "Validate token",
      description = "Validates a JWT token and returns its information")
  @PostMapping("/validate")
  public ResponseEntity<Map<String, Object>> validateToken(
      @RequestBody Map<String, String> request) {
    String token = request.get("token");
    if (token == null || token.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
    }

    if (tokenValidationService == null) {
      return ResponseEntity.status(503)
          .body(Map.of("error", "Keycloak authentication not available"));
    }

    JwtValidationResult validation = tokenValidationService.validateToken(token);
    return ResponseEntity.ok(userInfoMapper.toTokenValidationResponse(validation));
  }

  /** Check if user has specific role */
  @Operation(
      summary = "Check user role",
      description = "Checks if the current user has a specific role",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/has-role/{role}")
  public ResponseEntity<Map<String, Object>> hasRole(
      @PathVariable String role, Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
      return ResponseEntity.status(401).body(Map.of("error", "Invalid authentication"));
    }

    if (tokenValidationService == null) {
      return ResponseEntity.status(503)
          .body(Map.of("error", "Keycloak authentication not available"));
    }

    String token = jwtToken.getJwtToken();
    Map<String, Object> result = new HashMap<>();
    result.put("hasRole", tokenValidationService.hasRole(token, role));
    result.put("role", role);
    result.put("username", tokenValidationService.extractUsername(token));

    return ResponseEntity.ok(result);
  }

  /** Get user authorities/roles */
  @Operation(
      summary = "Get user authorities",
      description = "Retrieves all authorities/roles for the current user",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/authorities")
  public ResponseEntity<Map<String, Object>> getAuthorities(Authentication authentication) {
    Map<String, Object> result = new HashMap<>();
    result.put("authorities", authentication.getAuthorities());
    result.put("username", authentication.getName());
    result.put("authenticated", authentication.isAuthenticated());

    return ResponseEntity.ok(result);
  }
}
