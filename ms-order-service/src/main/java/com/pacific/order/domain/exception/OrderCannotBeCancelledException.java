package com.pacific.order.domain.exception;

/**
 * Exception thrown when attempting to cancel an order that cannot be cancelled
 */
public class OrderCannotBeCancelledException extends RuntimeException {
    
    public OrderCannotBeCancelledException(String message) {
        super(message);
    }

    public OrderCannotBeCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}

