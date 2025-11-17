package com.pacific.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error response DTO for validation errors. Extends ErrorResponse to include field-level validation
 * errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {

  /** Timestamp when the error occurred */
  private LocalDateTime timestamp;

  /** HTTP status code (typically 400) */
  private Integer status;

  /** HTTP status reason phrase */
  private String error;

  /** General error message */
  private String message;

  /** Request path where the error occurred */
  private String path;

  /** Field-level validation errors Key: field name, Value: error message */
  private Map<String, String> fieldErrors;

  /** Trace ID for debugging (optional) */
  private String traceId;
}

