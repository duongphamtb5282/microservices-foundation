package com.pacific.auth.modules.user.dto.response;

import com.pacific.auth.modules.role.entity.RoleType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for User entity. This DTO is designed to avoid circular references during
 * serialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

  private UUID id;
  private String userName;
  private String email;
  private String password;
  private Instant createdAt;
  private String createdBy;
  private Instant modifiedAt;
  private String modifiedBy;

  // Only include role names, not the full Role objects to avoid circular references
  private Set<RoleType> roleNames;

  /** Get role names in Spring Security format (ROLE_*) */
  public Set<String> getAuthorities() {
    if (roleNames == null) {
      return Set.of();
    }
    return roleNames.stream()
        .map(RoleType::getAuthority)
        .collect(java.util.stream.Collectors.toSet());
  }
}
