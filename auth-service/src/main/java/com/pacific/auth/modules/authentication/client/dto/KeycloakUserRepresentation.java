package com.pacific.auth.modules.authentication.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Keycloak User Representation */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserRepresentation {

  private String id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private Boolean enabled;
  private Boolean emailVerified;
  private Long createdTimestamp;
  private List<KeycloakCredentialRepresentation> credentials;
  private List<String> realmRoles;
  private Map<String, List<String>> attributes;
}
