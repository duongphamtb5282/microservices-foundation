package com.pacific.auth.config.security;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for security endpoint paths. Externalizes endpoint patterns to make them
 * configurable.
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth-service.security.endpoints")
public class SecurityEndpointsProperties {

  /** Public endpoints that don't require authentication */
  private List<String> publicEndpoints = new ArrayList<>();

  /** Admin endpoints that require ADMIN role */
  private List<String> adminEndpoints = new ArrayList<>();

  /**
   * User endpoints that require USER/ADMIN role. These are grouped so that authorization rules can
   * be applied consistently.
   */
  private List<String> userEndpoints = new ArrayList<>();

  /** Authentication endpoints for rate limiting */
  private List<String> authenticationEndpoints = new ArrayList<>();

  /** Keycloak endpoints */
  private List<String> keycloakEndpoints = new ArrayList<>();

  /** Check if a path matches any pattern in the list */
  public boolean matchesAny(List<String> patterns, String path) {
    if (patterns == null || path == null) {
      return false;
    }

    for (String pattern : patterns) {
      if (path.contains(pattern.replace("/**", "").replace("**", ""))) {
        return true;
      }
    }
    return false;
  }

  /** Check if a path is a public endpoint */
  public boolean isPublicEndpoint(String path) {
    return matchesAny(publicEndpoints, path);
  }

  /** Check if a path is an admin endpoint */
  public boolean isAdminEndpoint(String path) {
    return matchesAny(adminEndpoints, path);
  }

  /** Check if a path is an authentication endpoint */
  public boolean isAuthenticationEndpoint(String path) {
    return matchesAny(authenticationEndpoints, path) || matchesAny(keycloakEndpoints, path);
  }

  /** Check if a path is a user endpoint */
  public boolean isUserEndpoint(String path) {
    return matchesAny(userEndpoints, path);
  }

  public String[] publicEndpointsArray() {
    return toArray(publicEndpoints);
  }

  public String[] adminEndpointsArray() {
    return toArray(adminEndpoints);
  }

  public String[] userEndpointsArray() {
    return toArray(userEndpoints);
  }

  public String[] keycloakEndpointsArray() {
    return toArray(keycloakEndpoints);
  }

  private String[] toArray(List<String> endpoints) {
    return endpoints == null || endpoints.isEmpty()
        ? new String[0]
        : endpoints.toArray(String[]::new);
  }
}
