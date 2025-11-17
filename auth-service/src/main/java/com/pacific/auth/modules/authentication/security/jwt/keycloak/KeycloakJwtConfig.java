package com.pacific.auth.modules.authentication.security.jwt.keycloak;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Keycloak JWT configuration for token validation. */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "auth-service.security.authentication.keycloak.enabled",
    havingValue = "true",
    matchIfMissing = false)
@Slf4j
public class KeycloakJwtConfig {

  private final KeycloakProperties keycloakProperties;

  // Inject the standard Spring Security properties from your application.yml
  //    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  //    private String issuerUri;
  //
  //    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  //    private String jwkSetUri;

  //    @Bean("keycloakJwtDecoder")
  //    public JwtDecoder keycloakJwtDecoder() {
  //        log.info("--- CONFIGURING KEYCLOAK JWT DECODER ---");
  //        log.info("JWK Set URI: [{}]", jwkSetUri);
  //        log.info("Expected Issuer (iss): [{}]", issuerUri);
  //
  //        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
  //
  //        // THE FIX: Validate the token's 'iss' claim against the 'issuerUri' property.
  //        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
  //
  //        // Set the validator on the decoder. This is what throws the "iss claim is not valid"
  // error if they don't match.
  //        jwtDecoder.setJwtValidator(withIssuer);
  //
  //        log.info("--- Keycloak JWT Decoder configured successfully. ---");
  //        return jwtDecoder;
  //    }

  /**
   * JWT Decoder configured for Keycloak Uses qualified bean name to avoid conflicts with custom JWT
   * decoder
   */
  @Bean("keycloakJwtDecoder")
  public JwtDecoder keycloakJwtDecoder() {
    try {

      String jwkSetUri = keycloakProperties.getIssuerUri() + "/protocol/openid-connect/certs";
      // String jwkSetUri =
      // "http://localhost:8080/realms/auth-service/protocol/openid-connect/certs";
      log.info("Configuring Keycloak JWT Decoder with JWK Set URI: {}", jwkSetUri);

      // Create JWK source for Keycloak
      JWKSource<SecurityContext> jwkSource =
          new RemoteJWKSet<>(new URL(keycloakProperties.getJwkSetUri()));

      // Create JWT processor
      DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

      // Configure key selector for supported algorithms
      JWSKeySelector<SecurityContext> keySelector =
          new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
      jwtProcessor.setJWSKeySelector(keySelector);

      // Create Nimbus JWT decoder
      NimbusJwtDecoder decoder = new NimbusJwtDecoder(jwtProcessor);

      // Configure validators
      // NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
      decoder.setJwtValidator(
          JwtValidators.createDefaultWithIssuer(keycloakProperties.getIssuerUri()));

      log.info("Keycloak JWT Decoder configured successfully");
      log.info("JWK Set URI: {}", keycloakProperties.getJwkSetUri());
      log.info("Issuer URI: {}", keycloakProperties.getIssuerUri());

      return decoder;

    } catch (MalformedURLException e) {
      log.error("Invalid Keycloak URL configuration", e);
      throw new RuntimeException("Failed to configure Keycloak JWT decoder", e);
    }
  }
}
