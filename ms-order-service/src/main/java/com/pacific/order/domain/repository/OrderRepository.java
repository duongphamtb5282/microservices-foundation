package com.pacific.order.domain.repository;

import com.pacific.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Order aggregate (Domain Layer - Port)
 * Implementation will be in infrastructure layer
 */
public interface OrderRepository {
    
    /**
     * Save an order
     */
    Order save(Order order);

    /**
     * Find order by ID
     */
    Optional<Order> findById(String id);

    /**
     * Find all orders for a user
     */
    List<Order> findByUserId(String userId);

    /**
     * Check if order exists
     */
    boolean existsById(String id);

    /**
     * Delete an order
     */
    void deleteById(String id);

    /**
     * Find all orders
     */
    List<Order> findAll();
}

