package com.pacific.auth.common.exception;

/** Exception thrown when attempting to create a role whose name already exists. */
public class RoleAlreadyExistsException extends RuntimeException {

  public RoleAlreadyExistsException(String message) {
    super(message);
  }

  public RoleAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
