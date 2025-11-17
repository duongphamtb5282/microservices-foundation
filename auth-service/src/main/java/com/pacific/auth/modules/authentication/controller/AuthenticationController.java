package com.pacific.auth.modules.authentication.controller;

import com.pacific.auth.modules.authentication.dto.request.AuthenticationRequestDto;
import com.pacific.auth.modules.authentication.dto.request.RefreshTokenRequestDto;
import com.pacific.auth.modules.authentication.dto.response.AuthenticationResponseDto;
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

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user management")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

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
    try {
      log.debug("Retrieved user info fordfdsfdfdsfdsf");
      //            if (authentication instanceof JwtAuthenticationToken jwtToken) {
      //                String token = jwtToken.getJwtToken();
      //                log.info("Retrieved user info for1");
      //                // For Keycloak tokens, use OAuth2TokenValidationService
      //                if (jwtToken.isKeycloakJwtToken()) {
      //                    if (tokenValidationService == null) {
      //                        return ResponseEntity.status(503).body(Map.of("error", "Keycloak
      // authentication not available"));
      //                    }
      //                    log.info("Retrieved user info for2");
      //                    JwtValidationResult validation =
      // tokenValidationService.validateToken(token);
      //
      //                    if (validation.isValid()) {
      //                        Map<String, Object> userInfo = new HashMap<>();
      //                        userInfo.put("username", validation.getUsername());
      //                        userInfo.put("email", validation.getEmail());
      //                        userInfo.put("firstName", validation.getFirstName());
      //                        userInfo.put("lastName", validation.getLastName());
      //                        userInfo.put("roles", validation.getRoles());
      //                        userInfo.put("issuer", validation.getIssuer());
      //                        userInfo.put("issuedAt", validation.getIssuedAt());
      //                        userInfo.put("expiresAt", validation.getExpiresAt());
      //                        userInfo.put("tokenType", jwtToken.getTokenType());
      //
      //                        log.info("Retrieved user info for: {} (Token type: {})",
      // validation.getUsername(), jwtToken.getTokenType());
      //                        return ResponseEntity.ok(userInfo);
      //                    } else {
      //                        log.warn("Invalid Keycloak token for user info request");
      //                        return ResponseEntity.status(401).body(Map.of("error",
      // validation.getErrorMessage()));
      //                    }
      //                } else {
      //                    log.info("kkkkkkk");
      //                    // For custom JWT tokens, extract info directly from authentication
      //                    Map<String, Object> userInfo = new HashMap<>();
      //                    userInfo.put("username", authentication.getName());
      //                    userInfo.put("authorities", authentication.getAuthorities().stream()
      //                            .map(auth -> auth.getAuthority())
      //                            .toList());
      //                    userInfo.put("tokenType", jwtToken.getTokenType());
      //                    userInfo.put("isCustomJwt", true);
      //                    userInfo.put("isKeycloakJwt", false);
      //
      //                    log.info("Retrieved user info for: {} (Token type: {})",
      // authentication.getName(), jwtToken.getTokenType());
      //                    return ResponseEntity.ok(userInfo);
      //                }
      //            } else {
      //                log.warn("Invalid authentication type for user info request: {}",
      // authentication.getClass().getSimpleName());
      //                return ResponseEntity.status(401).body(Map.of("error", "Invalid
      // authentication"));
      //            }
    } catch (Exception e) {
      log.error("Error retrieving user info", e);
      return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
    }
    return null;
  }

  /** Validate a token and return its information */
  @Operation(
      summary = "Validate token",
      description = "Validates a JWT token and returns its information")
  @PostMapping("/validate")
  public ResponseEntity<Map<String, Object>> validateToken(
      @RequestBody Map<String, String> request) {
    try {
      String token = request.get("token");
      if (token == null || token.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
      }

      if (tokenValidationService == null) {
        return ResponseEntity.status(503)
            .body(Map.of("error", "Keycloak authentication not available"));
      }

      JwtValidationResult validation = tokenValidationService.validateToken(token);

      Map<String, Object> result = new HashMap<>();
      result.put("valid", validation.isValid());

      if (validation.isValid()) {
        result.put("username", validation.getUsername());
        result.put("email", validation.getEmail());
        result.put("firstName", validation.getFirstName());
        result.put("lastName", validation.getLastName());
        result.put("roles", validation.getRoles());
        result.put("issuer", validation.getIssuer());
        result.put("issuedAt", validation.getIssuedAt());
        result.put("expiresAt", validation.getExpiresAt());
      } else {
        result.put("error", validation.getErrorMessage());
      }

      return ResponseEntity.ok(result);

    } catch (Exception e) {
      log.error("Error validating token", e);
      return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
    }
  }

  /** Check if user has specific role */
  @Operation(
      summary = "Check user role",
      description = "Checks if the current user has a specific role",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/has-role/{role}")
  public ResponseEntity<Map<String, Object>> hasRole(
      @PathVariable String role, Authentication authentication) {
    try {
      if (authentication instanceof JwtAuthenticationToken jwtToken) {
        String token = jwtToken.getJwtToken();
        if (tokenValidationService == null) {
          return ResponseEntity.status(503)
              .body(Map.of("error", "Keycloak authentication not available"));
        }
        boolean hasRole = tokenValidationService.hasRole(token, role);

        Map<String, Object> result = new HashMap<>();
        result.put("hasRole", hasRole);
        result.put("role", role);
        result.put("username", tokenValidationService.extractUsername(token));

        return ResponseEntity.ok(result);
      } else {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid authentication"));
      }
    } catch (Exception e) {
      log.error("Error checking role", e);
      return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
    }
  }

  /** Get user authorities/roles */
  @Operation(
      summary = "Get user authorities",
      description = "Retrieves all authorities/roles for the current user",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/authorities")
  public ResponseEntity<Map<String, Object>> getAuthorities(Authentication authentication) {
    try {
      Map<String, Object> result = new HashMap<>();
      result.put("authorities", authentication.getAuthorities());
      result.put("username", authentication.getName());
      result.put("authenticated", authentication.isAuthenticated());

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Error retrieving authorities", e);
      return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
    }
  }
}
