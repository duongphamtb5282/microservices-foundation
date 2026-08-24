package com.pacific.customer.service;

import com.pacific.customer.domain.exception.CustomerAlreadyExistsException;
import com.pacific.customer.domain.model.*;
import com.pacific.customer.infrastructure.persistence.document.CustomerDocument;
import com.pacific.customer.infrastructure.persistence.repository.CustomerRepository;
import com.pacific.shared.events.UserCreatedEvent;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for handling customer operations. Provides business logic for customer management and
 * event processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

  private final CustomerRepository customerRepository;

  /**
   * Creates a customer from a UserCreatedEvent. This is called when a user registers in the auth
   * service.
   *
   * @param event the UserCreatedEvent from auth service
   * @param correlationId correlation ID for tracing
   * @return Mono<Void> indicating completion
   */
  @Timed(
      value = "customer.create_from_user_event",
      description = "Time taken to create customer from user registration event",
      histogram = true)
  public Mono<Void> createCustomerFromUserEvent(UserCreatedEvent event, String correlationId) {
    log.info(
        "Creating customer from user registration event. User ID: {}, Email: {}, Correlation ID: {}",
        event.getUserId(),
        event.getEmail(),
        correlationId);

    return customerRepository
        .existsByEmail(event.getEmail())
        .flatMap(
            exists -> {
              if (exists) {
                log.info(
                    "Customer already exists for email: {}. Skipping creation. Correlation ID: {}",
                    event.getEmail(),
                    correlationId);
                return Mono.empty();
              }

              // Create new customer from user event
              Customer newCustomer = createCustomerFromUserEvent(event);
              CustomerDocument document = CustomerDocument.fromDomain(newCustomer);

              return customerRepository
                  .save(document)
                  .onErrorResume(
                      error -> isDuplicateKeyError(error),
                      error -> {
                        // Racy duplicate (F-27): the existsByEmail check passed but a concurrent
                        // event/request already created this email (unique index violation).
                        // Treat as already-created so the consumer can acknowledge - the end
                        // state (customer exists for the email) is already reached.
                        log.warn(
                            "Duplicate email {} detected on insert (race). Treating as already created. Correlation ID: {}",
                            event.getEmail(),
                            correlationId);
                        return Mono.empty();
                      })
                  .doOnSuccess(
                      saved -> {
                        if (saved == null) {
                          return; // duplicate-race path, already logged above
                        }
                        String customerId = saved.getId() != null ? saved.getId() : "unknown";
                        log.info(
                            "Customer created successfully from user event. Customer ID: {}, User ID: {}, Correlation ID: {}",
                            customerId,
                            event.getUserId(),
                            correlationId);
                      })
                  .doOnError(
                      error -> {
                        log.error(
                            "Failed to create customer from user event. User ID: {}, Email: {}, Correlation ID: {}, Error: {}",
                            event.getUserId(),
                            event.getEmail(),
                            correlationId,
                            error.getMessage(),
                            error);
                      })
                  .then();
            });
  }

  /**
   * Detects MongoDB duplicate-key errors (F-27). Spring Data MongoDB reactive translates the
   * driver's write error (code 11000) into Spring's DuplicateKeyException; the raw driver exception
   * (com.mongodb.DuplicateKeyException) is also handled in case it surfaces unwrapped.
   */
  private boolean isDuplicateKeyError(Throwable error) {
    if (error == null) {
      return false;
    }
    if (error instanceof DuplicateKeyException
        || error instanceof com.mongodb.DuplicateKeyException) {
      return true;
    }
    return error.getCause() != null && isDuplicateKeyError(error.getCause());
  }

  /**
   * Creates a Customer domain object from UserCreatedEvent. Uses default values for profile and
   * preferences since user event only contains basic info.
   */
  private Customer createCustomerFromUserEvent(UserCreatedEvent event) {
    // Extract name from username (assuming username is in format "firstname.lastname" or similar)
    String[] nameParts = parseNameFromUsername(event.getUsername());
    String firstName = nameParts[0];
    String lastName = nameParts[1];

    // Create basic profile with available information
    CustomerProfile profile =
        new CustomerProfile(
            firstName,
            lastName,
            null, // phone not available in user event
            null // dateOfBirth not available in user event
            );

    // Use default preferences
    CustomerPreferences preferences = CustomerPreferences.defaultPreferences();

    // Create the customer
    return Customer.createNew(event.getEmail(), profile, preferences);
  }

  /**
   * Parses first and last name from username. Handles various username formats like: - "john.doe"
   * -> "John", "Doe" - "johndoe" -> "John", "Doe" (if longer than 2 chars) - "john" -> "John", ""
   * (fallback)
   */
  private String[] parseNameFromUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      return new String[] {"Unknown", "User"};
    }

    String cleanUsername = username.trim().toLowerCase();

    // Try to split on common separators
    if (cleanUsername.contains(".")) {
      String[] parts = cleanUsername.split("\\.", 2);
      return capitalizeNames(parts);
    }

    if (cleanUsername.contains("_")) {
      String[] parts = cleanUsername.split("_", 2);
      return capitalizeNames(parts);
    }

    if (cleanUsername.contains("-")) {
      String[] parts = cleanUsername.split("-", 2);
      return capitalizeNames(parts);
    }

    // If no separator, try to intelligently split the name
    if (cleanUsername.length() > 2) {
      // Simple heuristic: first part is usually shorter
      for (int i = 1; i < cleanUsername.length() - 1; i++) {
        String firstPart = cleanUsername.substring(0, i);
        String secondPart = cleanUsername.substring(i);

        // If second part looks like a surname (longer or ends with common surname patterns)
        if (secondPart.length() >= 2 || secondPart.matches(".*(son|sen|man|berg|stein|ski)$")) {
          return new String[] {capitalize(firstPart), capitalize(secondPart)};
        }
      }
    }

    // Fallback: treat whole username as first name
    return new String[] {capitalize(cleanUsername), ""};
  }

  private String[] capitalizeNames(String[] parts) {
    String firstName = parts.length > 0 ? capitalize(parts[0]) : "Unknown";
    String lastName = parts.length > 1 ? capitalize(parts[1]) : "";
    return new String[] {firstName, lastName};
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }

  /** Find customer by ID */
  public Mono<Customer> findCustomerById(String id) {
    log.info("Finding customer by ID: {}", id);
    return customerRepository
        .findById(id)
        .map(CustomerDocument::toDomain)
        .doOnSuccess(
            customer -> log.info("Found customer: {}", customer != null ? customer.id() : "null"))
        .doOnError(
            error -> log.error("Error finding customer by ID {}: {}", id, error.getMessage()));
  }

  /** Find customer by email */
  public Mono<Customer> findCustomerByEmail(String email) {
    log.info("Finding customer by email: {}", email);
    return customerRepository
        .findByEmail(email)
        .map(CustomerDocument::toDomain)
        .doOnSuccess(
            customer -> log.info("Found customer: {}", customer != null ? customer.id() : "null"))
        .doOnError(
            error ->
                log.error("Error finding customer by email {}: {}", email, error.getMessage()));
  }

  /** Get all customers (for development/testing only) */
  public Flux<Customer> findAllCustomers() {
    log.info("Finding all customers");
    return customerRepository
        .findAll()
        .map(CustomerDocument::toDomain)
        .doOnComplete(() -> log.info("Retrieved all customers"))
        .doOnError(error -> log.error("Error finding all customers: {}", error.getMessage()));
  }

  /** Create a new customer manually (not from user event) */
  public Mono<Customer> createCustomerManually(
      String email, CustomerProfile profile, CustomerPreferences preferences) {
    log.info("Creating customer manually: {}", email);
    return customerRepository
        .existsByEmail(email)
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(new CustomerAlreadyExistsException(email));
              }
              Customer newCustomer = Customer.createNew(email, profile, preferences);
              CustomerDocument document = CustomerDocument.fromDomain(newCustomer);
              return customerRepository
                  .save(document)
                  .onErrorResume(
                      error -> isDuplicateKeyError(error),
                      error ->
                          // Racy duplicate (F-27): the unique index rejected the insert after the
                          // existsByEmail check passed. Surface a typed conflict (mapped to
                          // HTTP 409 in CustomerController).
                          Mono.error(new CustomerAlreadyExistsException(email, error)))
                  .map(
                      saved -> {
                        log.info("Customer created manually with ID: {}", saved.getId());
                        return saved.toDomain();
                      });
            })
        .doOnError(
            error ->
                log.error("Error creating customer manually {}: {}", email, error.getMessage()));
  }

  /** Update customer profile */
  public Mono<Customer> updateCustomerProfile(String id, CustomerProfile newProfile) {
    log.info("Updating customer profile for ID: {}", id);
    return findCustomerById(id)
        .flatMap(
            customer -> {
              // Reset version to the as-loaded value: @Version saves match {_id, version: <as
              // loaded>} and increment during save, so a pre-increment would fail the match.
              Customer updatedCustomer =
                  customer.withProfile(newProfile).withVersion(customer.version());
              CustomerDocument document = CustomerDocument.fromDomain(updatedCustomer);
              return customerRepository
                  .save(document)
                  .map(
                      saved -> {
                        log.info("Customer profile updated for ID: {}", id);
                        return saved.toDomain();
                      });
            })
        .doOnError(
            error ->
                log.error("Error updating customer profile for ID {}: {}", id, error.getMessage()));
  }

  /** Update customer preferences */
  public Mono<Customer> updateCustomerPreferences(String id, CustomerPreferences newPreferences) {
    log.info("Updating customer preferences for ID: {}", id);
    return findCustomerById(id)
        .flatMap(
            customer -> {
              // Reset version to the as-loaded value: @Version saves match {_id, version: <as
              // loaded>} and increment during save, so a pre-increment would fail the match.
              Customer updatedCustomer =
                  customer.withPreferences(newPreferences).withVersion(customer.version());
              CustomerDocument document = CustomerDocument.fromDomain(updatedCustomer);
              return customerRepository
                  .save(document)
                  .map(
                      saved -> {
                        log.info("Customer preferences updated for ID: {}", id);
                        return saved.toDomain();
                      });
            })
        .doOnError(
            error ->
                log.error(
                    "Error updating customer preferences for ID {}: {}", id, error.getMessage()));
  }

  /**
   * Update customer profile and preferences in a single document save (atomic).
   *
   * <p>Partial-update fix (5b): profile and preferences live on the SAME customer document, so both
   * are applied in one save instead of two independent saves (previously, a failure of the second
   * save left the first committed). Reactive MongoDB transactions require a replica set (not
   * available in the demo), so the single-document write is the correct atomicity boundary.
   */
  public Mono<Customer> updateCustomer(
      String id, CustomerProfile newProfile, CustomerPreferences newPreferences) {
    log.info("Updating customer profile and preferences for ID: {}", id);
    return findCustomerById(id)
        .flatMap(
            customer -> {
              // Reset version to the as-loaded value: @Version saves match {_id, version: <as
              // loaded>} and increment during save, so a pre-increment would fail the match.
              Customer updatedCustomer =
                  customer
                      .withProfile(newProfile)
                      .withPreferences(newPreferences)
                      .withVersion(customer.version());
              CustomerDocument document = CustomerDocument.fromDomain(updatedCustomer);
              return customerRepository.save(document).map(CustomerDocument::toDomain);
            })
        .doOnSuccess(customer -> log.info("Customer updated for ID: {}", id))
        .doOnError(
            error -> log.error("Error updating customer for ID {}: {}", id, error.getMessage()));
  }

  /** Delete customer by ID */
  public Mono<Void> deleteCustomer(String id) {
    log.info("Deleting customer by ID: {}", id);
    return customerRepository
        .deleteById(id)
        .doOnSuccess(unused -> log.info("Customer deleted successfully: {}", id))
        .doOnError(error -> log.error("Error deleting customer {}: {}", id, error.getMessage()));
  }
}
