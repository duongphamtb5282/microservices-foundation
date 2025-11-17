package com.pacific.core.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited. When applied to a method, the AuditAspect will
 * automatically record audit events for that method execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

  /** The type of entity being audited. */
  String entityType();

  /** The audit action being performed. */
  AuditAction action();

  /** Additional details about the audit event. */
  String details() default "";
}
