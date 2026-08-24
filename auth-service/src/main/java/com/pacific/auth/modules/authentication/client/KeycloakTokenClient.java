package com.pacific.auth.modules.authentication.client;

import com.pacific.auth.modules.authentication.client.dto.KeycloakTokenResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client for Keycloak Token API Handles token generation, refresh, introspection, and
 * revocation
 *
 * <p>Form parameters travel as a single {@code @RequestBody Map} (form-urlencoded). With individual
 * {@code @RequestParam} arguments the encoder put the values into the query string and sent an
 * EMPTY body — Keycloak silently rejected that shape with {@code 400 invalid_request} (no event in
 * the Keycloak log). A Map argument is written into the body by {@code FormHttpMessageConverter},
 * which is the shape Keycloak's token endpoint expects.
 */
@FeignClient(
    name = "keycloak-token",
    url = "${auth-service.security.authentication.keycloak.server-url:http://localhost:8080}",
    configuration = KeycloakFeignConfig.class)
public interface KeycloakTokenClient {

  /** Get access token using password grant */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/token",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  KeycloakTokenResponse getToken(
      @PathVariable("realm") String realm, @RequestBody Map<String, ?> form);

  /** Refresh access token */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/token",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  KeycloakTokenResponse refreshToken(
      @PathVariable("realm") String realm, @RequestBody Map<String, ?> form);

  /** Get service account token (client credentials grant) */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/token",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  KeycloakTokenResponse getServiceAccountToken(
      @PathVariable("realm") String realm, @RequestBody Map<String, ?> form);

  /** Logout (revoke refresh token) */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/logout",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  void logout(@PathVariable("realm") String realm, @RequestBody Map<String, ?> form);

  /** Revoke token (access or refresh token) */
  @PostMapping(
      value = "/realms/{realm}/protocol/openid-connect/revoke",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  void revokeToken(@PathVariable("realm") String realm, @RequestBody Map<String, ?> form);
}
