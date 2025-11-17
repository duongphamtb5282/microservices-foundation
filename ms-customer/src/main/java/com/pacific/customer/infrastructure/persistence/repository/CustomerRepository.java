package com.pacific.customer.infrastructure.persistence.repository;

import com.pacific.customer.infrastructure.persistence.document.CustomerDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Reactive MongoDB repository for Customer entities. Provides reactive database operations for
 * customer management.
 */
@Repository
public interface CustomerRepository extends ReactiveMongoRepository<CustomerDocument, String> {

  /**
   * Find a customer by email address.
   *
   * @param email the email address to search for
   * @return Mono containing the customer document if found, empty otherwise
   */
  Mono<CustomerDocument> findByEmail(String email);

  /**
   * Check if a customer exists with the given email.
   *
   * @param email the email address to check
   * @return Mono containing true if customer exists, false otherwise
   */
  Mono<Boolean> existsByEmail(String email);
}
