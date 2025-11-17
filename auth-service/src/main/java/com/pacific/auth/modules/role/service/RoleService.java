package com.pacific.auth.modules.role.service;

import com.pacific.auth.modules.role.entity.Role;
import com.pacific.auth.modules.role.entity.RoleType;
import com.pacific.auth.modules.role.repository.RoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing roles. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoleService {

  private final RoleRepository roleRepository;

  /**
   * Find all roles
   *
   * @return List of all roles
   */
  @PreAuthorize("hasRole('ADMIN')")
  public List<Role> findAll() {
    log.debug("Finding all roles");
    return roleRepository.findAll();
  }

  /**
   * Find role by ID
   *
   * @param id the role ID
   * @return Optional containing the role if found
   */
  public Optional<Role> findById(UUID id) {
    log.debug("Finding role by ID: {}", id);
    return roleRepository.findById(id);
  }

  /**
   * Find role by name
   *
   * @param name the role name
   * @return Optional containing the role if found
   */
  public Optional<Role> findByName(RoleType name) {
    log.debug("Finding role by name: {}", name);
    return roleRepository.findByName(name);
  }

  /**
   * Find roles by user ID
   *
   * @param userId the user ID
   * @return Set of roles for the user
   */
  public Set<Role> findByUserId(UUID userId) {
    log.debug("Finding roles for user ID: {}", userId);
    return roleRepository.findByUserId(userId);
  }

  /**
   * Find roles by user email
   *
   * @param email the user email
   * @return Set of roles for the user
   */
  public Set<Role> findByUserEmail(String email) {
    log.debug("Finding roles for user email: {}", email);
    return roleRepository.findByUserEmail(email);
  }

  /**
   * Create a new role
   *
   * @param role the role to create
   * @return the created role
   */
  @Transactional
  public Role createRole(Role role) {
    log.info("Creating new role: {}", role.getName());

    if (roleRepository.existsByName(role.getName())) {
      throw new IllegalArgumentException("Role with name '" + role.getName() + "' already exists");
    }

    return roleRepository.save(role);
  }

  /**
   * Update an existing role
   *
   * @param role the role to update
   * @return the updated role
   */
  @Transactional
  public Role updateRole(Role role) {
    log.info("Updating role: {}", role.getName());

    if (!roleRepository.existsById(role.getId())) {
      throw new IllegalArgumentException("Role with ID '" + role.getId() + "' not found");
    }

    return roleRepository.save(role);
  }

  /**
   * Delete a role by ID
   *
   * @param id the role ID
   */
  @Transactional
  public void deleteRole(UUID id) {
    log.info("Deleting role with ID: {}", id);

    if (!roleRepository.existsById(id)) {
      throw new IllegalArgumentException("Role with ID '" + id + "' not found");
    }

    roleRepository.deleteById(id);
  }

  /**
   * Check if role exists by name
   *
   * @param name the role name
   * @return true if role exists
   */
  public boolean existsByName(RoleType name) {
    return roleRepository.existsByName(name);
  }

  /**
   * Get or create a role by name
   *
   * @param name the role name
   * @return the role
   */
  @Transactional
  public Role getOrCreateRole(RoleType name) {
    log.debug("Getting or creating role: {}", name);

    return roleRepository
        .findByName(name)
        .orElseGet(
            () -> {
              log.info("Creating new role: {}", name);
              Role newRole = new Role(name);
              return roleRepository.save(newRole);
            });
  }
}
