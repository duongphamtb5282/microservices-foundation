package com.pacific.auth.modules.database.service;

import com.pacific.auth.common.exception.DatabaseAccessException;
import com.pacific.core.service.BaseDatabaseService;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Auth-service specific database service. Extends BaseDatabaseService with auth-specific database
 * operations. Single responsibility: Handle auth-specific database operations.
 */
@Slf4j
@Service
public class AuthDatabaseService extends BaseDatabaseService {

  public AuthDatabaseService(DataSource dataSource) {
    super(dataSource);
  }

  @Override
  protected String getServiceName() {
    return "auth-service";
  }

  @Override
  protected String getSchemaName() {
    return "auth";
  }

  @Override
  protected List<Class<?>> getEntityClasses() {
    return List.of(
        com.pacific.auth.modules.user.entity.User.class,
        com.pacific.auth.modules.role.entity.Role.class,
        com.pacific.auth.modules.permission.entity.Permission.class,
        com.pacific.auth.modules.permission.entity.RolePermission.class);
  }

  /** Check if user exists in database. Single responsibility: Check user existence. */
  public boolean userExists(String username) {
    try {
      String sql = "SELECT COUNT(*) FROM auth.tbl_user WHERE user_name = ?";
      try (var connection = dataSource.getConnection();
          var statement = connection.prepareStatement(sql)) {
        statement.setString(1, username);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getInt(1) > 0;
          }
        }
      }
    } catch (Exception e) {
      log.error("Error checking if user exists: {}", username, e);
      throw new DatabaseAccessException("Failed to check if user exists: " + username, e);
    }
    return false;
  }

  /** Check if email exists in database. Single responsibility: Check email existence. */
  public boolean emailExists(String email) {
    try {
      String sql = "SELECT COUNT(*) FROM auth.tbl_user WHERE email = ?";
      try (var connection = dataSource.getConnection();
          var statement = connection.prepareStatement(sql)) {
        statement.setString(1, email);
        try (var resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getInt(1) > 0;
          }
        }
      }
    } catch (Exception e) {
      log.error("Error checking if email exists: {}", email, e);
      throw new DatabaseAccessException("Failed to check if email exists: " + email, e);
    }
    return false;
  }

  /** Get user count from database. Single responsibility: Get user count. */
  public long getUserCount() {
    try {
      String sql = "SELECT COUNT(*) FROM auth.tbl_user";
      try (var connection = dataSource.getConnection();
          var statement = connection.prepareStatement(sql);
          var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong(1);
        }
      }
    } catch (Exception e) {
      log.error("Error getting user count", e);
      throw new DatabaseAccessException("Failed to get user count", e);
    }
    return 0;
  }

  /** Get role count from database. Single responsibility: Get role count. */
  public long getRoleCount() {
    try {
      String sql = "SELECT COUNT(*) FROM auth.tbl_role";
      try (var connection = dataSource.getConnection();
          var statement = connection.prepareStatement(sql);
          var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong(1);
        }
      }
    } catch (Exception e) {
      log.error("Error getting role count", e);
      throw new DatabaseAccessException("Failed to get role count", e);
    }
    return 0;
  }

  /** Get permission count from database. Single responsibility: Get permission count. */
  public long getPermissionCount() {
    try {
      String sql = "SELECT COUNT(*) FROM auth.tbl_permission";
      try (var connection = dataSource.getConnection();
          var statement = connection.prepareStatement(sql);
          var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return resultSet.getLong(1);
        }
      }
    } catch (Exception e) {
      log.error("Error getting permission count", e);
      throw new DatabaseAccessException("Failed to get permission count", e);
    }
    return 0;
  }

  /** Get database statistics. Single responsibility: Get database statistics. */
  public String getDatabaseStats() {
    return String.format(
        "Auth Database Stats - Users: %d, Roles: %d, Permissions: %d",
        getUserCount(), getRoleCount(), getPermissionCount());
  }
}
