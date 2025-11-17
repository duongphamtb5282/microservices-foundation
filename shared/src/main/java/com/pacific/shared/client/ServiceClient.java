package com.pacific.shared.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pacific.shared.dto.ApiResponse;
import com.pacific.shared.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

/** Generic service client for inter-service communication */
@Slf4j
@Component
public class ServiceClient {

  private final RestTemplate restTemplate;

  @Value("${services.auth.url:http://localhost:8083}")
  private String authServiceUrl;

  @Value("${services.foundation.url:http://localhost:8082}")
  private String foundationServiceUrl;

  public ServiceClient() {
    this.restTemplate = new RestTemplate();
  }

  /** Make a GET request to another service */
  public <T> ApiResponse<T> get(String serviceUrl, String endpoint, Class<T> responseType) {
    try {
      String url = serviceUrl + endpoint;
      log.debug("Making GET request to: {}", url);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<String> entity = new HttpEntity<>(headers);
      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        T data = JsonUtils.fromJson(response.getBody(), responseType);
        return ApiResponse.success(data, "Request successful");
      } else {
        return ApiResponse.error("Request failed with status: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("Error making GET request to {}: {}", serviceUrl + endpoint, e.getMessage());
      return ApiResponse.error("Service communication failed: " + e.getMessage());
    }
  }

  /** Make a POST request to another service */
  public <T> ApiResponse<T> post(
      String serviceUrl, String endpoint, Object requestBody, Class<T> responseType) {
    try {
      String url = serviceUrl + endpoint;
      log.debug("Making POST request to: {}", url);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      String jsonBody = JsonUtils.toJson(requestBody);
      HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

      ResponseEntity<String> response =
          restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        T data = JsonUtils.fromJson(response.getBody(), responseType);
        return ApiResponse.success(data, "Request successful");
      } else {
        return ApiResponse.error("Request failed with status: " + response.getStatusCode());
      }
    } catch (Exception e) {
      log.error("Error making POST request to {}: {}", serviceUrl + endpoint, e.getMessage());
      return ApiResponse.error("Service communication failed: " + e.getMessage());
    }
  }

  /** Get auth service URL */
  public String getAuthServiceUrl() {
    return authServiceUrl;
  }

  /** Get foundation service URL */
  public String getFoundationServiceUrl() {
    return foundationServiceUrl;
  }
}
