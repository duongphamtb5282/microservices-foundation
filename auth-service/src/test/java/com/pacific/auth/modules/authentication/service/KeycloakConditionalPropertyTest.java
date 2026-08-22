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
package com.pacific.auth.modules.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.pacific.auth.modules.authentication.client.KeycloakAdminClient;
import com.pacific.auth.modules.authentication.client.KeycloakTokenClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Regression test for S-06: the Keycloak stack (service + Feign clients) must activate on the same
 * canonical property key as the JWT validation stack ({@code
 * auth-service.security.authentication.keycloak.enabled}).
 */
class KeycloakConditionalPropertyTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withBean(KeycloakTokenClient.class, () -> mock(KeycloakTokenClient.class))
          .withBean(KeycloakAdminClient.class, () -> mock(KeycloakAdminClient.class))
          .withUserConfiguration(KeycloakService.class);

  @Test
  void keycloakServiceActivatesOnCanonicalEnabledKey() {
    runner
        .withPropertyValues("auth-service.security.authentication.keycloak.enabled=true")
        .run(ctx -> assertThat(ctx).hasSingleBean(KeycloakService.class));
  }

  @Test
  void keycloakServiceIsDisabledWithoutProperty() {
    runner.run(ctx -> assertThat(ctx).doesNotHaveBean(KeycloakService.class));
  }
}
