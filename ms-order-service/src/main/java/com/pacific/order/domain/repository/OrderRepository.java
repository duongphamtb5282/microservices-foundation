package com.pacific.order.domain.repository;

import com.pacific.order.domain.model.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository interface for Order aggregate (Domain Layer - Port) Implementation will be in
 * infrastructure layer
 */
public interface OrderRepository {

  /** Save an order */
  Order save(Order order);

  /** Find order by ID */
  Optional<Order> findById(String id);

  /** Find all orders for a user */
  List<Order> findByUserId(String userId);

  /**
   * DB-side paginated view of a user's orders (F-19). {@code Page}/{@code Pageable} leak Spring
   * Data into the domain port as a deliberate trade-off: they give us LIMIT/OFFSET + count in one
   * query and page metadata without re-inventing pagination in the domain layer.
   */
  Page<Order> findByUserId(String userId, Pageable pageable);

  /** Check if order exists */
  boolean existsById(String id);

  /** Delete an order */
  void deleteById(String id);
}
