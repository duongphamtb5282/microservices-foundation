package com.pacific.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Standardized error response DTO. Returned by GlobalExceptionHandler for all exceptions. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  /** Timestamp when the error occurred */
  private LocalDateTime timestamp;

  /** HTTP status code */
  private Integer status;

  /** HTTP status reason phrase (e.g., "Bad Request", "Not Found") */
  private String error;

  /** Human-readable error message */
  private String message;

  /** Request path where the error occurred */
  private String path;

  /** Additional error details (optional) Useful for providing context-specific information */
  private Map<String, String> details;

  /** Trace ID for debugging (optional) Can be used for correlating logs */
  private String traceId;
}

