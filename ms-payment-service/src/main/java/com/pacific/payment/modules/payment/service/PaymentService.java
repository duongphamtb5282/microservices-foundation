package com.pacific.payment.modules.payment.service;

import com.pacific.payment.modules.payment.domain.Payment;

/**
 * Service interface for Payment operations
 */
public interface PaymentService {
    
    /**
     * Check if payment exists by order ID
     */
    boolean existsByOrderId(String orderId);

    /**
     * Process payment via payment gateway (simulation)
     * Returns true if successful, false otherwise
     */
    boolean processPayment(Payment payment);
}

