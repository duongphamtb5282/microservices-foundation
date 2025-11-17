package com.pacific.auth.modules.authentication.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Keycloak Role Representation */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakRoleRepresentation {

  private String id;
  private String name;
  private String description;
  private Boolean composite;
}
