package com.pacific.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration properties for OpenAPI documentation. Externalizes API documentation metadata. */
@Data
@Component
@ConfigurationProperties(prefix = "openapi")
public class OpenApiProperties {

  /** API title */
  private String title = "Java Spring Boot API";

  /** API description */
  private String description = "Java Spring Boot REST API Documentation";

  /** API version */
  private String version = "1.0.0";

  /** License information */
  private final License license = new License();

  /** Server URLs for API */
  private List<String> serverUrls = new ArrayList<>();

  @Data
  public static class License {
    private String name = "Apache 2.0";
    private String url = "https://www.apache.org/licenses/LICENSE-2.0";
  }
}
