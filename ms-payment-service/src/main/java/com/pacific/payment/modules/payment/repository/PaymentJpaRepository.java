package com.pacific.payment.modules.payment.repository;

import com.pacific.payment.modules.payment.entity.PaymentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA Repository for PaymentEntity */
@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, String> {

  /** Find payment by order ID */
  Optional<PaymentEntity> findByOrderId(String orderId);

  /** Check if payment exists by order ID */
  boolean existsByOrderId(String orderId);
}
