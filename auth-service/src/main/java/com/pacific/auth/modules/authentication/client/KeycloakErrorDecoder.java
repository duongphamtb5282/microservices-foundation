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
    String message = extractErrorMessage(response);

    log.error("Keycloak API error: {} - {} for {}", status, message, methodKey);

    // Map Keycloak errors to appropriate exceptions
    return switch (status) {
      case UNAUTHORIZED ->
          new ResponseStatusException(
              HttpStatus.UNAUTHORIZED, "Keycloak authentication failed: " + message);
      case FORBIDDEN ->
          new ResponseStatusException(
              HttpStatus.FORBIDDEN, "Keycloak authorization failed: " + message);
      case NOT_FOUND ->
          new ResponseStatusException(
              HttpStatus.NOT_FOUND, "Keycloak resource not found: " + message);
      case CONFLICT ->
          new ResponseStatusException(
              HttpStatus.CONFLICT, "Resource already exists in Keycloak: " + message);
      case BAD_REQUEST ->
          new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "Invalid request to Keycloak: " + message);
      default -> defaultErrorDecoder.decode(methodKey, response);
    };
  }

  /** Extract error message from response body */
  private String extractErrorMessage(Response response) {
    try {
      if (response.body() != null) {
        String body = new String(response.body().asInputStream().readAllBytes());
        // Try to parse JSON error response
        if (body.contains("error_description")) {
          int start = body.indexOf("error_description") + 19;
          int end = body.indexOf("\"", start + 1);
          if (end > start) {
            return body.substring(start, end);
          }
        } else if (body.contains("errorMessage")) {
          int start = body.indexOf("errorMessage") + 15;
          int end = body.indexOf("\"", start + 1);
          if (end > start) {
            return body.substring(start, end);
          }
        }
        return body.substring(0, Math.min(body.length(), 200));
      }
    } catch (Exception e) {
      log.warn("Failed to extract error message from Keycloak response", e);
    }
    return "Unknown error";
  }
}
