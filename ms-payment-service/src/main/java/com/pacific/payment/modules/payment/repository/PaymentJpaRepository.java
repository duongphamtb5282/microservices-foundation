package com.pacific.payment.modules.payment.repository;

import com.pacific.payment.modules.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for PaymentEntity
 */
@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, String> {
    
    /**
     * Find payment by order ID
     */
    Optional<PaymentEntity> findByOrderId(String orderId);

    /**
     * Find all payments for a user
     */
    List<PaymentEntity> findByUserId(String userId);

    /**
     * Check if payment exists by order ID
     */
    boolean existsByOrderId(String orderId);
}

