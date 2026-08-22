package com.pacific.payment.modules.payment.repository;

import com.pacific.payment.modules.payment.domain.Payment;
import java.util.Optional;

/** Repository interface for Payment */
public interface PaymentRepository {

  /** Save a payment */
  Payment save(Payment payment);

  /** Find payment by ID */
  Optional<Payment> findById(String id);

  /** Find payment by order ID */
  Optional<Payment> findByOrderId(String orderId);

  /** Check if payment exists by order ID */
  boolean existsByOrderId(String orderId);
}
