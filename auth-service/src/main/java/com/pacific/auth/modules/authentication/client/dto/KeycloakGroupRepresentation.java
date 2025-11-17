package com.pacific.auth.modules.authentication.client.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Keycloak group representation DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakGroupRepresentation {

  private String id;

  private String name;

  private String path;

  private List<KeycloakGroupRepresentation> subGroups;

  private Map<String, List<String>> attributes;

  private List<String> realmRoles;

  private Map<String, List<String>> clientRoles;
}
