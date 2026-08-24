package com.pacific.auth.common.exception;

import com.pacific.auth.common.dto.ErrorResponse;
import com.pacific.auth.common.dto.ValidationErrorResponse;
import com.pacific.shared.exceptions.ValidationException;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler for auth-service. Intercepts and handles all exceptions thrown in
 * controllers. Provides consistent error response format across the application.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // ============================================================================
  // Business Logic Exceptions
  // ============================================================================

  /**
   * Handle UserAlreadyExistsException Thrown when attempting to register a user with existing
   * email/username
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
      UserAlreadyExistsException ex, WebRequest request) {
    log.warn("User already exists: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .details(ex.getDetails())
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /** Handle UserNotFoundException Thrown when user is not found in the system */
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(
      UserNotFoundException ex, WebRequest request) {
    log.debug("User not found: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /** Handle RoleNotFoundException Thrown when a role is not found in the system */
  @ExceptionHandler(RoleNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleRoleNotFound(
      RoleNotFoundException ex, WebRequest request) {
    log.debug("Role not found: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /** Handle RoleAlreadyExistsException Thrown when attempting to create a duplicate role */
  @ExceptionHandler(RoleAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleRoleAlreadyExists(
      RoleAlreadyExistsException ex, WebRequest request) {
    log.warn("Role already exists: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /** Handle TokenExpiredException Thrown when JWT token has expired */
  @ExceptionHandler(TokenExpiredException.class)
  public ResponseEntity<ErrorResponse> handleTokenExpired(
      TokenExpiredException ex, WebRequest request) {
    log.warn("Token expired: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  /** Handle InvalidTokenException Thrown when JWT token is invalid or malformed */
  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidToken(
      InvalidTokenException ex, WebRequest request) {
    log.warn("Invalid token: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  // ============================================================================
  // Validation Exceptions
  // ============================================================================

  /** Handle ValidationException Thrown when business validation fails */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ValidationErrorResponse> handleValidationException(
      ValidationException ex, WebRequest request) {
    log.warn("Validation error: {}", ex.getMessage());

    ValidationErrorResponse errorResponse =
        ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .fieldErrors(ex.getValidationErrors())
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /** Handle MethodArgumentNotValidException Thrown when @Valid annotation validation fails */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, WebRequest request) {
    log.warn("Validation error: {}", ex.getMessage());

    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              fieldErrors.put(fieldName, errorMessage);
            });

    ValidationErrorResponse errorResponse =
        ValidationErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Request validation failed")
            .path(request.getDescription(false).replace("uri=", ""))
            .fieldErrors(fieldErrors)
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  // ============================================================================
  // Security Exceptions
  // ============================================================================

  /** Handle BadCredentialsException Thrown when login credentials are invalid */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(
      BadCredentialsException ex, WebRequest request) {
    log.warn("Bad credentials: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .message("Invalid username or password")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  /** Handle AuthenticationException Generic authentication errors */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex, WebRequest request) {
    log.warn("Authentication error: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.UNAUTHORIZED.value())
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .message("Authentication failed")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  // ============================================================================
  // Feign Client Exceptions (Keycloak)
  // ============================================================================

  /** Handle FeignException Thrown when Feign client calls to Keycloak fail */
  @ExceptionHandler(FeignException.class)
  public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex, WebRequest request) {
    // Sanitized (8d): log the full response body for diagnostics, return a fixed message.
    log.error("Keycloak API error: status={}, body={}", ex.status(), safeContent(ex), ex);

    HttpStatus status = HttpStatus.valueOf(ex.status());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message("Authentication service error")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(status).body(errorResponse);
  }

  /** Handle ResponseStatusException Thrown by Keycloak error decoder and other components */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(
      ResponseStatusException ex, WebRequest request) {
    log.warn("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(ex.getStatusCode().value())
            .error(ex.getStatusCode().toString())
            .message(ex.getReason() != null ? ex.getReason() : "An error occurred")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
  }

  // ============================================================================
  // Generic Exceptions
  // ============================================================================

  /** Handle IllegalArgumentException Thrown for invalid method arguments */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request) {
    log.warn("Illegal argument: {}", ex.getMessage());

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  /**
   * Missing/invalid required headers or path variables (e.g. no Authorization header on
   * /api/users/**, /api/roles, /api/cache/**) are client errors, not 500s. Without this handler
   * the catch-all below turns them into a 500 "An unexpected error occurred" — the exact bug fixed
   * in order's OrderControllerAdvice for POST /orders (missing Authorization header).
   */
  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<ErrorResponse> handleRequestBinding(
      ServletRequestBindingException ex, WebRequest request) {
    log.warn("Invalid request binding: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  /** Malformed request body (bad JSON, wrong field type) is a client error, not a 500. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException ex, WebRequest request) {
    log.warn("Unreadable request body: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request);
  }

  /** Wrong Content-Type is a client error (415), not a 500. */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, WebRequest request) {
    log.warn("Unsupported media type: {}", ex.getMessage());
    return buildErrorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type not supported", request);
  }

  /** Wrong HTTP verb (e.g. POST to a GET-only path) is a client error (405), not a 500. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, WebRequest request) {
    log.warn("Method not supported: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request);
  }

  /** Handle all other exceptions Catch-all for unexpected errors */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
    log.error("Unexpected error occurred", ex);

    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message("An unexpected error occurred. Please try again later.")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /** Safely read the Feign exception response body for logging (never sent to clients). */
  private String safeContent(FeignException ex) {
    try {
      String responseBody = ex.contentUTF8();
      return (responseBody == null || responseBody.isEmpty()) ? "(empty)" : responseBody;
    } catch (Exception e) {
      log.warn("Failed to read Feign exception body", e);
      return "(unreadable)";
    }
  }

  /** Shared 4xx client-error response builder (same ErrorResponse shape as the other handlers). */
  private ResponseEntity<ErrorResponse> buildErrorResponse(
      HttpStatus status, String message, WebRequest request) {
    ErrorResponse errorResponse =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
    return ResponseEntity.status(status).body(errorResponse);
  }
}
