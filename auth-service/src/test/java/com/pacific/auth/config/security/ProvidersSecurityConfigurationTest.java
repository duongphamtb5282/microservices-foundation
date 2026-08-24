package com.pacific.auth.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Regression test for S-06: Keycloak is the single authentication provider. The dual-mode switch
 * (custom/keycloak/database) and the DAO provider were removed, so {@code authenticationManager()}
 * must register exactly the Keycloak provider and nothing else.
 */
class ProvidersSecurityConfigurationTest {

  private final CorsConfigurationSource corsConfigurationSource =
      mock(CorsConfigurationSource.class);
  private final SecurityEndpointsProperties securityEndpointsProperties =
      mock(SecurityEndpointsProperties.class);
  private final ApplicationContext applicationContext = mock(ApplicationContext.class);

  @Test
  void authenticationManagerRegistersOnlyKeycloakProvider() {
    KeycloakJwtAuthenticationProvider keycloakJwtProvider =
        mock(KeycloakJwtAuthenticationProvider.class);
    when(applicationContext.getBean(KeycloakJwtAuthenticationProvider.class))
        .thenReturn(keycloakJwtProvider);

    ProvidersSecurityConfiguration config =
        new ProvidersSecurityConfiguration(corsConfigurationSource, securityEndpointsProperties);
    // applicationContext is an @Autowired field, not constructor-injected
    ReflectionTestUtils.setField(config, "applicationContext", applicationContext);

    var providers =
        ((org.springframework.security.authentication.ProviderManager)
                config.authenticationManager())
            .getProviders();

    assertThat(providers).hasSize(1).containsExactly(keycloakJwtProvider);
  }

  @Test
  void authenticationManagerStillBuildsWhenKeycloakProviderMissing() {
    when(applicationContext.getBean(KeycloakJwtAuthenticationProvider.class))
        .thenThrow(
            new org.springframework.beans.factory.NoSuchBeanDefinitionException(
                "KeycloakJwtAuthenticationProvider"));

    ProvidersSecurityConfiguration config =
        new ProvidersSecurityConfiguration(corsConfigurationSource, securityEndpointsProperties);
    ReflectionTestUtils.setField(config, "applicationContext", applicationContext);

    var providers =
        ((org.springframework.security.authentication.ProviderManager)
                config.authenticationManager())
            .getProviders();

    assertThat(providers).isEmpty();
  }
}
