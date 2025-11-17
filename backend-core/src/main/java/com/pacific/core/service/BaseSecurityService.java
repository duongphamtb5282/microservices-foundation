package com.pacific.core.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Base security service with single responsibility. Provides common security operations that can be
 * extended by services.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseSecurityService {

  /** Check if user is authenticated. Single responsibility: Check authentication status. */
  public boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal());
  }

  /** Get current authenticated user. Single responsibility: Retrieve current user. */
  public String getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      return userDetails.getUsername();
    }
    return null;
  }

  /** Get current user roles. Single responsibility: Retrieve user roles. */
  public List<String> getCurrentUserRoles() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getAuthorities() != null) {
      return authentication.getAuthorities().stream()
          .map(authority -> authority.getAuthority())
          .toList();
    }
    return List.of();
  }

  /** Check if user has specific role. Single responsibility: Check user role. */
  public boolean hasRole(String role) {
    return getCurrentUserRoles().contains(role);
  }

  /** Check if user has any of the specified roles. Single responsibility: Check user roles. */
  public boolean hasAnyRole(String... roles) {
    List<String> userRoles = getCurrentUserRoles();
    for (String role : roles) {
      if (userRoles.contains(role)) {
        return true;
      }
    }
    return false;
  }

  /** Get authentication object. Single responsibility: Retrieve authentication object. */
  public Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  /**
   * Abstract method for service-specific public endpoints. Single responsibility: Define
   * service-specific public endpoints.
   */
  protected abstract List<String> getPublicEndpoints();

  /**
   * Abstract method for service-specific admin endpoints. Single responsibility: Define
   * service-specific admin endpoints.
   */
  protected abstract List<String> getAdminEndpoints();

  /**
   * Abstract method for service-specific security configuration. Single responsibility: Define
   * service-specific security behavior.
   */
  protected abstract String getServiceName();
}
