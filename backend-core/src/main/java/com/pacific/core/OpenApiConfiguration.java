package com.pacific.core;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI documentation. Uses externalized properties instead of hardcoded
 * values.
 */
@Configuration
@ConditionalOnClass(OpenAPI.class)
@RequiredArgsConstructor
public class OpenApiConfiguration {

  private final OpenApiProperties openApiProperties;

  /**
   * OpenAPI configuration bean with configurable metadata.
   *
   * @return the OpenAPI configuration
   */
  // Fallback only: a service that defines its own OpenAPI bean (e.g. ms-order's OpenApiConfig with
  // per-service branding) keeps it — without the gate, SpringDoc's openAPIBuilder saw two OpenAPI
  // beans and failed with NoUniqueBeanDefinitionException.
  @Bean
  @ConditionalOnMissingBean(OpenAPI.class)
  public OpenAPI customOpenAPI() {
    OpenAPI openAPI =
        new OpenAPI()
            .info(
                new Info()
                    .title(openApiProperties.getTitle())
                    .description(openApiProperties.getDescription())
                    .version(openApiProperties.getVersion())
                    .license(
                        new License()
                            .name(openApiProperties.getLicense().getName())
                            .url(openApiProperties.getLicense().getUrl())))
            .components(
                new Components()
                    .addSecuritySchemes(
                        "bearer-jwt",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .in(SecurityScheme.In.HEADER)
                            .name("Authorization")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));

    // Add configured server URLs if provided
    if (openApiProperties.getServerUrls() != null && !openApiProperties.getServerUrls().isEmpty()) {
      for (String serverUrl : openApiProperties.getServerUrls()) {
        openAPI.addServersItem(new Server().url(serverUrl));
      }
    }

    return openAPI;
  }
}
