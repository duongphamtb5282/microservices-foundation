package com.pacific.auth.modules.authentication.client;

import com.pacific.auth.modules.authentication.client.dto.KeycloakTokenResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign Client for Keycloak Token API Handles token generation, refresh, introspection, and
 * revocation
 */
@ConditionalOnProperty(name = "auth-service.security.keycloak.enabled", havingValue = "true")
@FeignClient(
    name = "keycloak-token",
    url = "${auth-service.security.keycloak.server-url:http://localhost:8080}",
    configuration = KeycloakFeignConfig.class)
public interface KeycloakTokenClient {

  /** Get access token using password grant */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/token",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  KeycloakTokenResponse getToken(
      @PathVariable("realm") String realm,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("username") String username,
      @RequestParam("password") String password,
      @RequestParam("grant_type") String grantType);

  /** Refresh access token */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/token",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  KeycloakTokenResponse refreshToken(
      @PathVariable("realm") String realm,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("refresh_token") String refreshToken,
      @RequestParam("grant_type") String grantType);

  /** Get service account token (client credentials grant) */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/token",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  KeycloakTokenResponse getServiceAccountToken(
      @PathVariable("realm") String realm,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("grant_type") String grantType);

  /** Logout (revoke refresh token) */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/logout",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  void logout(
      @PathVariable("realm") String realm,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("refresh_token") String refreshToken);

  /** Revoke token (access or refresh token) */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/revoke",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  void revokeToken(
      @PathVariable("realm") String realm,
      @RequestParam("client_id") String clientId,
      @RequestParam("client_secret") String clientSecret,
      @RequestParam("token") String token);
}
