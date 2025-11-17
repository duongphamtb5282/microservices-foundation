package com.pacific.auth.modules.role.controller;

import com.pacific.auth.modules.role.entity.Role;
import com.pacific.auth.modules.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing roles. Provides endpoints for role management operations. */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "APIs for managing user roles and permissions")
public class RoleController {

  private final RoleService roleService;

  /**
   * Get all roles. Requires ADMIN role to access this endpoint.
   *
   * @return List of all roles in the system
   */
  @GetMapping
  @Operation(
      summary = "Get all roles",
      description = "Retrieve a list of all available roles in the system")
  public ResponseEntity<List<Role>> getAllRoles() {
    log.info("🔍 Getting all roles");

    List<Role> roles = roleService.findAll();

    log.info("✅ Retrieved {} roles", roles.size());
    return ResponseEntity.ok(roles);
  }
}
