package com.pacific.gateway.interfaces.rest;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback target for the gateway's circuit breaker filter. When a circuit opens, the
 * CircuitBreaker filter forwards requests here (fallbackUri: forward:/fallback) and the caller
 * receives a fixed 503 response instead of an error without a body.
 */
@RestController
public class FallbackController {

  /**
   * Handles all HTTP methods on /fallback and returns a fixed 503 JSON body.
   *
   * @return 503 Service Unavailable with a fixed JSON error body
   */
  @RequestMapping("/fallback")
  public ResponseEntity<Map<String, Object>> fallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.<String, Object>of("status", 503, "error", "Service temporarily unavailable"));
  }
}
