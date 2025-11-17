package com.pacific.auth.modules.authentication.client.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Keycloak user session representation DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakUserSessionRepresentation {

  private String id;

  private String username;

  private String userId;

  private String ipAddress;

  private Long start;

  private Long lastAccess;

  private Map<String, String> clients;
}
