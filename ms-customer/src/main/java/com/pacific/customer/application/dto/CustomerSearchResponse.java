package com.pacific.customer.application.dto;

import com.pacific.customer.domain.model.Customer;
import com.pacific.customer.domain.model.CustomerStatus;
import java.time.Instant;
import java.util.List;

/** Response DTO for customer search results with pagination */
public record CustomerSearchResponse(
    List<CustomerSummary> customers,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious) {
  /** Summary DTO for search results (less data than full CustomerResponse) */
  public record CustomerSummary(
      String id,
      String email,
      String firstName,
      String lastName,
      CustomerStatus status,
      Instant createdAt) {
    /** Factory method to create from domain Customer */
    public static CustomerSummary from(Customer customer) {
      return new CustomerSummary(
          customer.id(),
          customer.email(),
          customer.profile().firstName(),
          customer.profile().lastName(),
          customer.status(),
          customer.createdAt());
    }
  }

  /** Factory method to create paginated response */
  public static CustomerSearchResponse create(
      List<Customer> customers, int page, int size, long totalElements) {

    List<CustomerSummary> summaries = customers.stream().map(CustomerSummary::from).toList();

    int totalPages = (int) Math.ceil((double) totalElements / size);
    boolean hasNext = (page + 1) < totalPages;
    boolean hasPrevious = page > 0;

    return new CustomerSearchResponse(
        summaries, page, size, totalElements, totalPages, hasNext, hasPrevious);
  }
}
