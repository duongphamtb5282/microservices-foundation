package com.pacific.shared.exceptions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.pacific.shared.exceptions.dto.ErrorResponseDto;
import com.pacific.shared.exceptions.dto.FieldErrorDto;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for centralized error handling across all controllers. This handler
 * catches all exceptions and returns standardized error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Handle custom validation exceptions */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponseDto> handleValidationException(
      ValidationException ex, HttpServletRequest request) {

    log.warn("Validation error: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message(ex.getMessage())
            .detail("Request validation failed")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .validationErrors(ex.getValidationErrors())
            .errorCode(ex.getErrorCode())
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Handle business logic exceptions */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponseDto> handleBusinessException(
      BusinessException ex, HttpServletRequest request) {

    log.warn("Business error: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Business Rule Violation")
            .message(ex.getMessage())
            .detail("Business logic validation failed")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode(ex.getErrorCode())
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Handle authentication exceptions */
  @ExceptionHandler({
    com.pacific.shared.exceptions.AuthenticationException.class,
    BadCredentialsException.class
  })
  public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
      Exception ex, HttpServletRequest request) {

    log.warn("Authentication error: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    String errorCode =
        ex instanceof com.pacific.shared.exceptions.AuthenticationException
            ? ((com.pacific.shared.exceptions.AuthenticationException) ex).getErrorCode()
            : "AUTHENTICATION_ERROR";

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.UNAUTHORIZED.value())
            .error("Authentication Failed")
            .message(ex.getMessage())
            .detail("User authentication failed")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode(errorCode)
            .build();

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  /** Handle authorization exceptions */
  @ExceptionHandler({
    com.pacific.shared.exceptions.AuthorizationException.class,
    AccessDeniedException.class
  })
  public ResponseEntity<ErrorResponseDto> handleAuthorizationException(
      Exception ex, HttpServletRequest request) {

    log.warn("Authorization error: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    String errorCode =
        ex instanceof com.pacific.shared.exceptions.AuthorizationException
            ? ((com.pacific.shared.exceptions.AuthorizationException) ex).getErrorCode()
            : "AUTHORIZATION_ERROR";

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message(ex.getMessage())
            .detail("User does not have permission to access this resource")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode(errorCode)
            .build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  /** Handle resource not found exceptions */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
      ResourceNotFoundException ex, HttpServletRequest request) {

    log.warn("Resource not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .error("Resource Not Found")
            .message(ex.getMessage())
            .detail("The requested resource was not found")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("RESOURCE_NOT_FOUND")
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /** Handle resource constraint exceptions */
  @ExceptionHandler(ResourceConstraintException.class)
  public ResponseEntity<ErrorResponseDto> handleResourceConstraintException(
      ResourceConstraintException ex, HttpServletRequest request) {

    log.warn("Resource constraint error: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.CONFLICT.value())
            .error("Resource Constraint Violation")
            .message(ex.getMessage())
            .detail("The operation violates a resource constraint")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("RESOURCE_CONSTRAINT_VIOLATION")
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /** Handle Spring validation exceptions (@Valid) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    log.warn(
        "Method argument validation error: {} - Path: {}",
        ex.getMessage(),
        request.getRequestURI());

    List<FieldErrorDto> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::mapFieldError)
            .collect(Collectors.toList());

    Map<String, String> validationErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing));

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Request validation failed")
            .detail("One or more fields have validation errors")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .validationErrors(validationErrors)
            .fieldErrors(fieldErrors)
            .errorCode("VALIDATION_ERROR")
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Handle bind exceptions */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponseDto> handleBindException(
      BindException ex, HttpServletRequest request) {

    log.warn("Bind validation error: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    List<FieldErrorDto> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(this::mapFieldError)
            .collect(Collectors.toList());

    Map<String, String> validationErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (existing, replacement) -> existing));

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Request validation failed")
            .detail("One or more fields have validation errors")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .validationErrors(validationErrors)
            .fieldErrors(fieldErrors)
            .errorCode("VALIDATION_ERROR")
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Handle data integrity violation exceptions */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex, HttpServletRequest request) {

    log.error("Data integrity violation: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    String message = "Data integrity constraint violation";
    if (ex.getMessage().contains("duplicate key")) {
      message = "A record with this information already exists";
    } else if (ex.getMessage().contains("foreign key")) {
      message = "Cannot perform this operation due to related data constraints";
    }

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.CONFLICT.value())
            .error("Data Integrity Violation")
            .message(message)
            .detail("The operation violates database constraints")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("DATA_INTEGRITY_VIOLATION")
            .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /** Handle HTTP message not readable exceptions */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    log.warn("HTTP message not readable: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Request Body")
            .message("Request body is not readable or malformed")
            .detail("Please check the request body format and content type")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("INVALID_REQUEST_BODY")
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Handle missing request parameter exceptions */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex, HttpServletRequest request) {

    log.warn("Missing request parameter: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Missing Required Parameter")
            .message("Required parameter '" + ex.getParameterName() + "' is missing")
            .detail("Please provide all required parameters")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("MISSING_PARAMETER")
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Handle method argument type mismatch exceptions */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

    log.warn(
        "Method argument type mismatch: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    String message =
        String.format(
            "Parameter '%s' should be of type %s",
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Parameter Type")
            .message(message)
            .detail("Please check the parameter types and values")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("INVALID_PARAMETER_TYPE")
            .build();

    return ResponseEntity.badRequest().body(errorResponse);
  }

  /** Handle HTTP request method not supported exceptions */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

    log.warn("HTTP method not supported: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    String message =
        String.format(
            "Method '%s' is not supported for this endpoint. Supported methods: %s",
            ex.getMethod(), String.join(", ", ex.getSupportedMethods()));

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.METHOD_NOT_ALLOWED.value())
            .error("Method Not Allowed")
            .message(message)
            .detail("Please use a supported HTTP method for this endpoint")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("METHOD_NOT_ALLOWED")
            .build();

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
  }

  /** Handle no handler found exceptions */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleNoHandlerFoundException(
      NoHandlerFoundException ex, HttpServletRequest request) {

    log.warn("No handler found: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .error("Endpoint Not Found")
            .message("The requested endpoint was not found")
            .detail("Please check the URL and try again")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("ENDPOINT_NOT_FOUND")
            .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  /** Handle all other exceptions */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleGenericException(
      Exception ex, HttpServletRequest request) {

    log.error(
        "Unexpected error occurred: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);

    ErrorResponseDto errorResponse =
        ErrorResponseDto.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .detail("Please try again later or contact support if the problem persists")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .method(request.getMethod())
            .errorCode("INTERNAL_SERVER_ERROR")
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  /** Map Spring FieldError to our FieldErrorDto */
  private FieldErrorDto mapFieldError(FieldError fieldError) {
    return FieldErrorDto.builder()
        .field(fieldError.getField())
        .rejectedValue(fieldError.getRejectedValue())
        .message(fieldError.getDefaultMessage())
        .code(fieldError.getCode())
        .build();
  }
}
