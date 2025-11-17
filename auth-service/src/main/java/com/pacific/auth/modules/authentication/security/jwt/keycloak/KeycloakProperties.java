package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration properties for Keycloak integration. */
@Data
@Component
@ConfigurationProperties(prefix = "auth-service.security.authentication.keycloak")
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class KeycloakProperties {

  /** Keycloak server URL */
  private String serverUrl;

  /** Realm name */
  private String realm = "auth-service";

  /** Client ID */
  private String clientId;

  /** Resource (client ID) - for backward compatibility */
  public String getResource() {
    return clientId;
  }

  /** Client secret */
  private String credentialsSecret;

  /** SSL required setting */
  private String sslRequired = "external";

  /** Whether to use resource role mappings */
  private boolean useResourceRoleMappings = true;

  /** Whether this is a bearer-only client */
  private boolean bearerOnly = true;

  /** Whether to verify token audience */
  private boolean verifyTokenAudience = true;

  /** Connection pool size */
  private int connectionPoolSize = 20;

  /** Whether to disable trust manager */
  private boolean disableTrustManager = false;

  /** Whether to allow any hostname */
  private boolean allowAnyHostname = false;

  /** Truststore path */
  private String truststore;

  /** Truststore password */
  private String truststorePassword;

  /** JWK Set URI for token validation */
  private String jwkSetUri;

  /** Issuer URI for token validation */
  private String issuerUri;

  //    public String getIssuerUri() {
  //        return serverUrl + "/realms/" + realm;
  //    }

  /** Token endpoint URI */
  public String getTokenUri() {
    return serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";
  }

  /** User info endpoint URI */
  public String getUserInfoUri() {
    return serverUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
  }
}
