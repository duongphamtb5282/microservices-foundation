package com.pacific.auth.modules.authentication.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Custom error decoder for Keycloak Feign clients Translates Keycloak API errors into Spring
 * exceptions
 */
@Slf4j
public class KeycloakErrorDecoder implements ErrorDecoder {

  private final ErrorDecoder defaultErrorDecoder = new Default();

  @Override
  public Exception decode(String methodKey, Response response) {
    HttpStatus status = HttpStatus.valueOf(response.status());
    String body = readResponseBody(response);

    // Sanitized (8d): the raw Keycloak body is logged for diagnostics but never propagated
    // to clients; exceptions carry only a generic message with the HTTP status.
    log.error("Keycloak API error: {} for {}, response body: {}", status, methodKey, body);

    // Map Keycloak errors to appropriate exceptions
    return switch (status) {
      case UNAUTHORIZED ->
          new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Keycloak request failed");
      case FORBIDDEN ->
          new ResponseStatusException(HttpStatus.FORBIDDEN, "Keycloak request failed");
      case NOT_FOUND ->
          new ResponseStatusException(HttpStatus.NOT_FOUND, "Keycloak request failed");
      case CONFLICT -> new ResponseStatusException(HttpStatus.CONFLICT, "Keycloak request failed");
      case BAD_REQUEST ->
          new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keycloak request failed");
      default -> defaultErrorDecoder.decode(methodKey, response);
    };
  }

  /** Read the raw response body (for logging only; not propagated to clients). */
  private String readResponseBody(Response response) {
    try {
      if (response.body() != null) {
        return new String(response.body().asInputStream().readAllBytes());
      }
    } catch (Exception e) {
      log.warn("Failed to read Keycloak response body", e);
    }
    return "Unknown error";
  }
}
