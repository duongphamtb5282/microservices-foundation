package com.pacific.auth.modules.authentication.mapper;

import com.pacific.auth.modules.authentication.security.jwt.common.JwtValidationResult;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Maps token-validation results and authentication objects into the user-info maps returned by the
 * authentication REST endpoints. Keeps response-shape logic out of the controller (review finding:
 * the controller previously built these maps inline).
 */
@Component
public class UserInfoMapper {

  /** Keycloak token claims -> user info response (GET /api/auth/me). */
  public Map<String, Object> toKeycloakUserInfo(JwtValidationResult validation, String tokenType) {
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("username", validation.getUsername());
    userInfo.put("email", validation.getEmail());
    userInfo.put("firstName", validation.getFirstName());
    userInfo.put("lastName", validation.getLastName());
    userInfo.put("roles", validation.getRoles());
    userInfo.put("issuer", validation.getIssuer());
    userInfo.put("issuedAt", validation.getIssuedAt());
    userInfo.put("expiresAt", validation.getExpiresAt());
    userInfo.put("tokenType", tokenType);
    return userInfo;
  }

  /** Custom JWT authentication -> user info response (GET /api/auth/me). */
  public Map<String, Object> toCustomJwtUserInfo(
      Authentication authentication, String tokenType) {
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("username", authentication.getName());
    userInfo.put(
        "authorities",
        authentication.getAuthorities().stream().map(auth -> auth.getAuthority()).toList());
    userInfo.put("tokenType", tokenType);
    userInfo.put("isCustomJwt", true);
    userInfo.put("isKeycloakJwt", false);
    return userInfo;
  }

  /** Token validation result -> validate-token response (POST /api/auth/validate). */
  public Map<String, Object> toTokenValidationResponse(JwtValidationResult validation) {
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
    return result;
  }
}
