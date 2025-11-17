package com.pacific.core.audit;

import java.lang.reflect.Method;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Aspect that automatically records audit events for service methods annotated with @Audit. */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

  private final AuditService auditService;

  /** Pointcut for methods annotated with @Audit. */
  @Pointcut("@annotation(com.pacific.core.audit.Audit)")
  public void auditAnnotatedMethods() {}

  /** Records audit events after successful method execution. */
  @AfterReturning(pointcut = "auditAnnotatedMethods()", returning = "result")
  public void recordAuditEvent(JoinPoint joinPoint, Object result) {
    try {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      Method method = signature.getMethod();

      Audit auditAnnotation = method.getAnnotation(Audit.class);
      if (auditAnnotation == null) {
        return;
      }

      String entityType = auditAnnotation.entityType();
      AuditAction action = auditAnnotation.action();
      String details = auditAnnotation.details();

      // Try to extract entity ID from method parameters or result
      UUID entityId = extractEntityId(joinPoint, result);
      String currentUser = getCurrentUser();

      if (entityId != null) {
        auditService.recordAuditEvent(entityType, entityId, action, currentUser, details);
      } else {
        log.warn("Could not extract entity ID for audit event in method: {}", method.getName());
      }

    } catch (Exception e) {
      log.error("Failed to record audit event", e);
    }
  }

  /** Extracts entity ID from method parameters or return value. */
  private UUID extractEntityId(JoinPoint joinPoint, Object result) {
    // First, try to get from return value if it's a BaseAuditEntity
    if (result instanceof BaseAuditEntity entity) {
      return entity.getId();
    }

    // Then, try to find UUID parameters
    Object[] args = joinPoint.getArgs();
    for (Object arg : args) {
      if (arg instanceof UUID uuid) {
        return uuid;
      }
      if (arg instanceof BaseAuditEntity entity) {
        return entity.getId();
      }
      if (arg instanceof String str && isValidUUID(str)) {
        return UUID.fromString(str);
      }
    }

    return null;
  }

  /** Checks if a string is a valid UUID. */
  private boolean isValidUUID(String str) {
    try {
      UUID.fromString(str);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /** Gets the current authenticated user. */
  private String getCurrentUser() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
      }
    } catch (Exception e) {
      log.warn("Failed to get current user for audit: {}", e.getMessage());
    }
    return "system";
  }
}
