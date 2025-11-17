package com.pacific.customer.application.controller;

import com.pacific.customer.application.dto.CreateCustomerRequest;
import com.pacific.customer.application.dto.CustomerResponse;
import com.pacific.customer.domain.model.CustomerPreferences;
import com.pacific.customer.domain.model.CustomerProfile;
import com.pacific.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

/**
 * REST API controller for customer operations.
 * Provides HTTP endpoints for customer management.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Get all customers (for development/testing only)
     */
    @GetMapping
    @Operation(summary = "Get all customers", description = "Retrieve a list of all customers")
    @SecurityRequirement(name = "bearerAuth")
    public Flux<CustomerResponse> getAllCustomers() {
        log.info("Getting all customers");
        // Note: In a real application, this would be paginated and filtered
        return customerService.findAllCustomers()
            .map(CustomerResponse::from)
            .doOnComplete(() -> log.info("Retrieved all customers successfully"));
    }

    /**
     * Get customer by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieve a specific customer by their ID")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<CustomerResponse>> getCustomerById(@PathVariable String id) {
        log.info("Getting customer by ID: {}", id);
        return customerService.findCustomerById(id)
            .map(customer -> ResponseEntity.ok(CustomerResponse.from(customer)))
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnError(error -> log.error("Error getting customer by ID {}: {}", id, error.getMessage()));
    }

    /**
     * Get customer by email
     */
    @GetMapping("/email/{email}")
    @Operation(summary = "Get customer by email", description = "Retrieve a specific customer by their email")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<CustomerResponse>> getCustomerByEmail(@PathVariable String email) {
        log.info("Getting customer by email: {}", email);
        return customerService.findCustomerByEmail(email)
            .map(customer -> ResponseEntity.ok(CustomerResponse.from(customer)))
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnError(error -> log.error("Error getting customer by email {}: {}", email, error.getMessage()));
    }

    /**
     * Create a new customer
     */
    @PostMapping
    @Operation(summary = "Create customer", description = "Create a new customer")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        log.info("Creating customer: {}", request.email());

        var profile = new CustomerProfile(
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.dateOfBirth()
        );

        var preferences = request.getCustomerPreferences();

        return customerService.createCustomerManually(request.email(), profile, preferences)
            .map(customer -> ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer)))
            .doOnSuccess(response -> log.info("Customer created successfully: {}", request.email()))
            .doOnError(error -> log.error("Error creating customer {}: {}", request.email(), error.getMessage()));
    }

    /**
     * Update customer profile and preferences
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Update an existing customer's profile and preferences")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody CreateCustomerRequest request) {
        log.info("Updating customer {}: {}", id, request.email());

        var newProfile = new CustomerProfile(
            request.firstName(),
            request.lastName(),
            request.phone(),
            request.dateOfBirth()
        );

        var newPreferences = request.getCustomerPreferences();

        // First update profile, then preferences
        return customerService.updateCustomerProfile(id, newProfile)
            .flatMap(updatedWithProfile ->
                customerService.updateCustomerPreferences(updatedWithProfile.id(), newPreferences))
            .map(customer -> ResponseEntity.ok(CustomerResponse.from(customer)))
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnError(error -> log.error("Error updating customer {}: {}", id, error.getMessage()));
    }

    /**
     * Delete customer
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Delete a customer by ID")
    @SecurityRequirement(name = "bearerAuth")
    public Mono<ResponseEntity<Void>> deleteCustomer(@PathVariable String id) {
        log.info("Deleting customer: {}", id);
        return customerService.findCustomerById(id)
            .flatMap(customer -> customerService.deleteCustomer(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build())))
            .defaultIfEmpty(ResponseEntity.notFound().build())
            .doOnError(error -> log.error("Error deleting customer {}: {}", id, error.getMessage()));
    }
}
