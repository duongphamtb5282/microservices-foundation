package com.pacific.auth.modules.authentication.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Keycloak Credential (Password) Representation */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakCredentialRepresentation {

  private String type; // "password"
  private String value;
  private Boolean temporary;
}
