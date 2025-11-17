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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
    log.error("Keycloak API error: {} - {}", ex.status(), ex.getMessage());

    String message = extractFeignErrorMessage(ex);
    HttpStatus status = HttpStatus.valueOf(ex.status());

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

  /** Extract error message from Feign exception */
  private String extractFeignErrorMessage(FeignException ex) {
    try {
      // Try to extract Keycloak error message from response body
      String responseBody = ex.contentUTF8();
      if (responseBody != null && !responseBody.isEmpty()) {
        // Parse JSON response for error_description or error fields
        if (responseBody.contains("error_description")) {
          int start = responseBody.indexOf("error_description") + 19;
          int end = responseBody.indexOf("\"", start + 1);
          return responseBody.substring(start, end);
        } else if (responseBody.contains("errorMessage")) {
          int start = responseBody.indexOf("errorMessage") + 15;
          int end = responseBody.indexOf("\"", start + 1);
          return responseBody.substring(start, end);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to extract error message from Feign exception", e);
    }

    // Fallback messages based on status code
    return switch (ex.status()) {
      case 401 -> "Keycloak authentication failed";
      case 403 -> "Keycloak authorization failed";
      case 404 -> "Keycloak resource not found";
      case 409 -> "Resource already exists in Keycloak";
      case 400 -> "Invalid request to Keycloak";
      default -> "Keycloak service error";
    };
  }
}
