package com.pacific.auth.modules.role.entity;

/**
 * Enum representing the different role types in the system. This provides type safety and prevents
 * hardcoded role strings.
 */
public enum RoleType {

  /** Administrator role with full system access */
  ADMIN("ADMIN", "Administrator role with full system access"),

  /** Regular user role with limited access */
  USER("USER", "Regular user role with limited access"),

  /** Moderator role with content management access */
  MODERATOR("MODERATOR", "Moderator role with content management access"),

  /** Guest role with read-only access */
  GUEST("GUEST", "Guest role with read-only access");

  private final String name;
  private final String description;

  RoleType(String name, String description) {
    this.name = name;
    this.description = description;
  }

  /**
   * Get the role name
   *
   * @return the role name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the role description
   *
   * @return the role description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the Spring Security authority for this role
   *
   * @return the authority string (e.g., "ROLE_ADMIN")
   */
  public String getAuthority() {
    return "ROLE_" + name;
  }

  /**
   * Find a role type by name (case-insensitive)
   *
   * @param name the role name to search for
   * @return the matching RoleType or null if not found
   */
  public static RoleType fromName(String name) {
    if (name == null) {
      return null;
    }

    for (RoleType roleType : values()) {
      if (roleType.name.equalsIgnoreCase(name)) {
        return roleType;
      }
    }
    return null;
  }

  /**
   * Check if a role name is valid
   *
   * @param name the role name to check
   * @return true if the role name is valid
   */
  public static boolean isValidRole(String name) {
    return fromName(name) != null;
  }

  @Override
  public String toString() {
    return name;
  }
}
