package com.pacific.order.infrastructure.persistence.repository;

import com.pacific.order.infrastructure.persistence.entity.OrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA Repository for OrderEntity */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {

  /** Find all orders for a user */
  List<OrderEntity> findByUserId(String userId);

  /** Check if order exists by ID */
  boolean existsById(String id);
}
