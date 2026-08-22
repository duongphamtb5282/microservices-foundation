/*
 * Copyright (c) 2025 Demo Company. All rights reserved.
 *
 * This file is part of the Microservices Demo project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pacific.auth.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pacific.auth.modules.authentication.security.jwt.custom.CustomJwtAuthenticationProvider;
import com.pacific.auth.modules.authentication.security.jwt.keycloak.KeycloakJwtAuthenticationProvider;
import com.pacific.auth.modules.user.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Regression test for S-06: the auth-mode switch in {@code authenticationManager()} must not fall
 * through from "custom" into "keycloak".
 */
class ProvidersSecurityConfigurationTest {

  private final UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
  private final CorsConfigurationSource corsConfigurationSource =
      mock(CorsConfigurationSource.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final SecurityEndpointsProperties securityEndpointsProperties =
      mock(SecurityEndpointsProperties.class);
  private final ApplicationContext applicationContext = mock(ApplicationContext.class);

  @Test
  void customModeMustNotRegisterKeycloakProvider() {
    // Both providers are available in the context, so a fall-through would be visible
    CustomJwtAuthenticationProvider customJwtProvider = mock(CustomJwtAuthenticationProvider.class);
    KeycloakJwtAuthenticationProvider keycloakJwtProvider =
        mock(KeycloakJwtAuthenticationProvider.class);
    when(applicationContext.getBean(
            "customJwtAuthenticationProvider", CustomJwtAuthenticationProvider.class))
        .thenReturn(customJwtProvider);
    when(applicationContext.getBean(KeycloakJwtAuthenticationProvider.class))
        .thenReturn(keycloakJwtProvider);

    ProvidersSecurityConfiguration config =
        new ProvidersSecurityConfiguration(
            userDetailsService,
            corsConfigurationSource,
            passwordEncoder,
            securityEndpointsProperties);
    // applicationContext is an @Autowired field, not constructor-injected
    ReflectionTestUtils.setField(config, "applicationContext", applicationContext);
    ReflectionTestUtils.setField(config, "authMode", "custom");

    // When: building the authentication manager in "custom" mode
    var providers =
        ((org.springframework.security.authentication.ProviderManager)
                config.authenticationManager())
            .getProviders();

    // Then: DAO + custom only - the keycloak provider must not leak in via fall-through
    assertThat(providers)
        .hasSize(2)
        .contains(customJwtProvider)
        .doesNotContain(keycloakJwtProvider);
  }
}
