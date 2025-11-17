package com.pacific.core.audit;

import java.time.Instant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enhanced audit entity listener that extends Spring Data JPA's AuditingEntityListener. This
 * ensures audit fields are properly populated for all entities extending BaseAuditEntity and
 * handles soft delete auditing.
 */
@Slf4j
public class AuditEntityListener extends AuditingEntityListener {

  @PrePersist
  public void prePersist(Object entity) {
    log.debug("Pre-persist audit for entity: {}", entity.getClass().getSimpleName());
    super.touchForCreate(entity);
  }

  @PreUpdate
  public void preUpdate(Object entity) {
    log.debug("Pre-update audit for entity: {}", entity.getClass().getSimpleName());

    // Handle soft delete auditing
    if (entity instanceof BaseAuditEntity baseEntity) {
      // If this is a soft delete operation (isDeleted changed to true)
      if (baseEntity.isDeleted() && baseEntity.getDeletedAt() == null) {
        String currentUser = getCurrentAuditor();
        baseEntity.setDeletedBy(currentUser);
        baseEntity.setDeletedAt(Instant.now());
        log.debug(
            "Soft delete audit recorded for entity {} by user {}",
            entity.getClass().getSimpleName(),
            currentUser);
      }
    }

    super.touchForUpdate(entity);
  }

  /**
   * Gets the current auditor (user) from Spring Security context. Falls back to "system" if no
   * authenticated user is found.
   *
   * @return the current auditor username
   */
  private String getCurrentAuditor() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
      }
    } catch (Exception e) {
      log.warn("Failed to get current auditor from SecurityContext: {}", e.getMessage());
    }
    return "system";
  }
}
