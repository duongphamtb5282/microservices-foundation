package com.pacific.order.interfaces.rest.advice;

import com.pacific.order.domain.exception.InvalidOrderException;
import com.pacific.order.domain.exception.OrderCannotBeCancelledException;
import com.pacific.order.domain.exception.OrderNotFoundException;
import com.pacific.order.interfaces.rest.dto.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Global exception handler for Order Controller */
@RestControllerAdvice
@Slf4j
public class OrderControllerAdvice {

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
    log.error("Order not found: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ex.getMessage(), "ORDER_NOT_FOUND"));
  }

  @ExceptionHandler(InvalidOrderException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidOrder(InvalidOrderException ex) {
    log.error("Invalid order: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), "INVALID_ORDER"));
  }

  @ExceptionHandler(OrderCannotBeCancelledException.class)
  public ResponseEntity<ApiResponse<Void>> handleOrderCannotBeCancelled(
      OrderCannotBeCancelledException ex) {
    log.error("Order cannot be cancelled: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResponse.error(ex.getMessage(), "ORDER_CANNOT_BE_CANCELLED"));
  }

  /** Invalid query/handler arguments (e.g. bad paging parameters) are client errors, not 500s. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Invalid argument: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), "INVALID_ARGUMENT"));
  }

  /**
   * Missing/invalid required headers or path variables (e.g. no Authorization header, a
   * {@code @PathVariable} that did not bind) are client errors, not 500s. Without this handler the
   * catch-all below turns them into "INTERNAL_ERROR" — exactly what happened with POST /orders when
   * the request omitted the required Authorization header (the client saw a 500 for what was a 400).
   */
  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<ApiResponse<Void>> handleRequestBinding(ServletRequestBindingException ex) {
    log.warn("Invalid request binding: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), "INVALID_REQUEST"));
  }

  /** Malformed request body (bad JSON, wrong field type) is a client error, not a 500. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
    log.warn("Unreadable request body: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResponse.error("Malformed request body", "INVALID_REQUEST"));
  }

  /** Path variable of the wrong type (e.g. a non-UUID in an orderId path segment) is a 400. */
  @ExceptionHandler(TypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(TypeMismatchException ex) {
    log.warn("Type mismatch: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), "INVALID_REQUEST"));
  }

  /** Wrong Content-Type is a client error (415), not a 500. */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex) {
    log.warn("Unsupported media type: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(ApiResponse.error("Content-Type not supported", "UNSUPPORTED_MEDIA_TYPE"));
  }

  /** Wrong HTTP verb (e.g. POST to a GET-only path) is a client error (405), not a 500. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    log.warn("Method not supported: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(ApiResponse.error("Method not allowed", "METHOD_NOT_ALLOWED"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    log.error("Validation errors: {}", errors);

    ApiResponse<Map<String, String>> response =
        ApiResponse.<Map<String, String>>builder()
            .success(false)
            .message("Validation failed")
            .data(errors)
            .errorCode("VALIDATION_ERROR")
            .build();

    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("An unexpected error occurred", "INTERNAL_ERROR"));
  }
}
