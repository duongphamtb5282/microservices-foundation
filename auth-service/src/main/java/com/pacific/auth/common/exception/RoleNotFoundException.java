package com.pacific.auth.common.exception;

/** Exception thrown when a role is not found in the system. */
public class RoleNotFoundException extends RuntimeException {

  public RoleNotFoundException(String message) {
    super(message);
  }

  public RoleNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Create exception for role not found by ID */
  public static RoleNotFoundException forId(String roleId) {
    return new RoleNotFoundException(String.format("Role not found with ID: %s", roleId));
  }
}
