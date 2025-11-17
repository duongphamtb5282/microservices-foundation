package com.pacific.payment.modules.payment.repository;

import com.pacific.payment.modules.payment.domain.Payment;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment
 */
public interface PaymentRepository {
    
    /**
     * Save a payment
     */
    Payment save(Payment payment);

    /**
     * Find payment by ID
     */
    Optional<Payment> findById(String id);

    /**
     * Find payment by order ID
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Find all payments for a user
     */
    List<Payment> findByUserId(String userId);

    /**
     * Check if payment exists by order ID
     */
    boolean existsByOrderId(String orderId);

    /**
     * Find all payments
     */
    List<Payment> findAll();
}

