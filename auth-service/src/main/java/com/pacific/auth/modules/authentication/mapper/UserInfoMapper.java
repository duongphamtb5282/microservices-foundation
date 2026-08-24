package com.pacific.auth.modules.authentication.mapper;

import com.pacific.auth.modules.authentication.security.jwt.common.JwtValidationResult;
import java.util.HashMap;
import java.util.Map;
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

  /**
   * Token validation result -> validate-token response (POST /api/auth/validate).
   *
   * <p>Contract with ms-order's AuthServiceClient (ValidateTokenResponse DTO): the response must
   * carry {@code valid}, {@code userId}, {@code username}, {@code message}. {@code username} is
   * JwtValidationResult.getUsername() = the JWT {@code sub} (the Keycloak user UUID); the actual
   * login name is the {@code preferred_username} claim. userId and the display username are
   * mapped explicitly so order can build CreateOrderCommand without nulls.
   */
  public Map<String, Object> toTokenValidationResponse(JwtValidationResult validation) {
    Map<String, Object> result = new HashMap<>();
    result.put("valid", validation.isValid());

    if (validation.isValid()) {
      // Keycloak access tokens always carry sub (user UUID) and preferred_username (login name).
      result.put("userId", validation.getClaimAsString("sub"));
      String displayUsername = validation.getClaimAsString("preferred_username");
      result.put(
          "username", displayUsername != null ? displayUsername : validation.getUsername());
      result.put("email", validation.getEmail());
      result.put("firstName", validation.getFirstName());
      result.put("lastName", validation.getLastName());
      result.put("roles", validation.getRoles());
      result.put("issuer", validation.getIssuer());
      result.put("issuedAt", validation.getIssuedAt());
      result.put("expiresAt", validation.getExpiresAt());
    } else {
      result.put("error", validation.getErrorMessage());
      result.put("message", validation.getErrorMessage());
    }
    return result;
  }
}
