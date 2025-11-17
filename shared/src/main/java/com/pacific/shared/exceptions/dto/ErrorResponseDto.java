package com.pacific.shared.exceptions.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

/**
 * Standardized error response DTO for all API errors. This provides a consistent error response
 * format across all endpoints.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {

  /** HTTP status code */
  private int status;

  /** Error type/category */
  private String error;

  /** Human-readable error message */
  private String message;

  /** Detailed error description */
  private String detail;

  /** Timestamp when the error occurred */
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private LocalDateTime timestamp;

  /** Request path that caused the error */
  private String path;

  /** Request method that caused the error */
  private String method;

  /** Validation errors (for validation failures) */
  private Map<String, String> validationErrors;

  /** Field-specific validation errors */
  private List<FieldErrorDto> fieldErrors;

  /** Additional error details */
  private Map<String, Object> additionalInfo;

  /** Error code for client-side handling */
  private String errorCode;

  /** Trace ID for debugging (if available) */
  private String traceId;
}
