package com.pacific.order.interfaces.rest.advice;

import com.pacific.order.domain.exception.InvalidOrderException;
import com.pacific.order.domain.exception.OrderCannotBeCancelledException;
import com.pacific.order.domain.exception.OrderNotFoundException;
import com.pacific.order.interfaces.rest.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Order Controller
 */
@RestControllerAdvice
@Slf4j
public class OrderControllerAdvice {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
        log.error("Order not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage(), "ORDER_NOT_FOUND"));
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrder(InvalidOrderException ex) {
        log.error("Invalid order: {}", ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(ex.getMessage(), "INVALID_ORDER"));
    }

    @ExceptionHandler(OrderCannotBeCancelledException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderCannotBeCancelled(OrderCannotBeCancelledException ex) {
        log.error("Order cannot be cancelled: {}", ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(ex.getMessage(), "ORDER_CANNOT_BE_CANCELLED"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.error("Validation errors: {}", errors);
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
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
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred", "INTERNAL_ERROR"));
    }
}

