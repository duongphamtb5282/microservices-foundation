package com.pacific.customer.application.query;

import com.pacific.customer.domain.model.CustomerStatus;
import com.pacific.shared.messaging.cqrs.query.Query;
import org.springframework.data.domain.Sort;

/** Query to search customers with pagination and filtering */
public record SearchCustomersQuery(
    String searchTerm,
    CustomerStatus status,
    int page,
    int size,
    String sortBy,
    Sort.Direction sortDirection)
    implements Query {

  public SearchCustomersQuery {
    if (page < 0) {
      throw new IllegalArgumentException("Page cannot be negative");
    }
    if (size <= 0 || size > 100) {
      throw new IllegalArgumentException("Size must be between 1 and 100");
    }
    if (sortBy != null && sortBy.trim().isEmpty()) {
      throw new IllegalArgumentException("Sort by cannot be empty if provided");
    }
  }

  @Override
  public String getCorrelationId() {
    return "SEARCH_CUSTOMERS_" + page + "_" + size;
  }

  @Override
  public String getQueryType() {
    return "SEARCH_CUSTOMERS";
  }

  @Override
  public void validate() {
    if (searchTerm != null && searchTerm.length() > 100) {
      throw new IllegalArgumentException("Search term is too long");
    }
    if (sortBy != null && !isValidSortField(sortBy)) {
      throw new IllegalArgumentException("Invalid sort field: " + sortBy);
    }
  }

  /** Gets the offset for pagination */
  public int getOffset() {
    return page * size;
  }

  /** Checks if search term is provided */
  public boolean hasSearchTerm() {
    return searchTerm != null && !searchTerm.trim().isEmpty();
  }

  /** Checks if status filter is applied */
  public boolean hasStatusFilter() {
    return status != null;
  }

  /** Gets default sort direction */
  public Sort.Direction getSortDirectionOrDefault() {
    return sortDirection != null ? sortDirection : Sort.Direction.ASC;
  }

  /** Gets default sort field */
  public String getSortByOrDefault() {
    return sortBy != null ? sortBy : "createdAt";
  }

  /** Validates sort field */
  private boolean isValidSortField(String field) {
    return switch (field) {
      case "createdAt", "updatedAt", "email", "firstName", "lastName", "status" -> true;
      default -> false;
    };
  }

  /** Factory method for simple search */
  public static SearchCustomersQuery simpleSearch(String searchTerm, int page, int size) {
    return new SearchCustomersQuery(searchTerm, null, page, size, "createdAt", Sort.Direction.DESC);
  }

  /** Factory method for status-based search */
  public static SearchCustomersQuery byStatus(CustomerStatus status, int page, int size) {
    return new SearchCustomersQuery(null, status, page, size, "createdAt", Sort.Direction.DESC);
  }
}
