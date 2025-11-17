package com.pacific.core.audit;

/** Enumeration of audit actions that can be performed on entities. */
public enum AuditAction {
  /** Entity was created */
  CREATED("CREATED"),

  /** Entity was updated */
  UPDATED("UPDATED"),

  /** Entity was soft deleted */
  DELETED("DELETED"),

  /** Entity was restored from soft delete */
  RESTORED("RESTORED"),

  /** Entity was permanently deleted */
  HARD_DELETED("HARD_DELETED"),

  /** Entity was viewed/accessed */
  VIEWED("VIEWED"),

  /** Bulk operation performed */
  BULK_OPERATION("BULK_OPERATION");

  private final String value;

  AuditAction(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
