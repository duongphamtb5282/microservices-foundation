package com.pacific.order.infrastructure.persistence.repository;

import com.pacific.order.infrastructure.persistence.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA Repository for OrderEntity */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {

  /**
   * Find all orders for a user, eager-fetching items (F-18: toDomain must not touch lazy items).
   */
  @EntityGraph(attributePaths = "items")
  List<OrderEntity> findByUserId(String userId);

  /** Eager-fetch items so the domain adapter can map outside a transaction (F-18). */
  @Override
  @EntityGraph(attributePaths = "items")
  Optional<OrderEntity> findById(String id);

  /**
   * Real DB-side pagination for a user's orders (F-19: replaces in-memory PageImpl slicing). The
   * count query runs in the same statement as the page, eager-fetching items.
   */
  @EntityGraph(attributePaths = "items")
  Page<OrderEntity> findByUserId(String userId, Pageable pageable);

  /** Check if order exists by ID */
  boolean existsById(String id);
}
