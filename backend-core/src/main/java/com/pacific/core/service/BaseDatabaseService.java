package com.pacific.core.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base database service with single responsibility. Provides common database operations that can be
 * extended by services.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseDatabaseService {

  protected final DataSource dataSource;

  /** Check database connectivity. Single responsibility: Check database connection. */
  public boolean isDatabaseConnected() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(5);
    } catch (SQLException e) {
      log.error("Database connection failed", e);
      return false;
    }
  }

  /** Get database URL. Single responsibility: Retrieve database URL. */
  public String getDatabaseUrl() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.getMetaData().getURL();
    } catch (SQLException e) {
      log.error("Failed to get database URL", e);
      return "Unknown";
    }
  }

  /** Get database product name. Single responsibility: Retrieve database product name. */
  public String getDatabaseProductName() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      log.error("Failed to get database product name", e);
      return "Unknown";
    }
  }

  /** Get database version. Single responsibility: Retrieve database version. */
  public String getDatabaseVersion() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.getMetaData().getDatabaseProductVersion();
    } catch (SQLException e) {
      log.error("Failed to get database version", e);
      return "Unknown";
    }
  }

  /** Generic find all operation. Single responsibility: Retrieve all entities. */
  @Transactional(readOnly = true)
  public <T> List<T> findAll(JpaRepository<T, ?> repository) {
    return repository.findAll();
  }

  /** Generic find by ID operation. Single responsibility: Retrieve entity by ID. */
  @Transactional(readOnly = true)
  public <T, ID> Optional<T> findById(JpaRepository<T, ID> repository, ID id) {
    return repository.findById(id);
  }

  /** Generic save operation. Single responsibility: Save entity. */
  @Transactional
  public <T> T save(JpaRepository<T, ?> repository, T entity) {
    return repository.save(entity);
  }

  /** Generic delete operation. Single responsibility: Delete entity. */
  @Transactional
  public <T> void delete(JpaRepository<T, ?> repository, T entity) {
    repository.delete(entity);
  }

  /** Generic delete by ID operation. Single responsibility: Delete entity by ID. */
  @Transactional
  public <T, ID> void deleteById(JpaRepository<T, ID> repository, ID id) {
    repository.deleteById(id);
  }

  /** Generic count operation. Single responsibility: Count entities. */
  @Transactional(readOnly = true)
  public <T> long count(JpaRepository<T, ?> repository) {
    return repository.count();
  }

  /**
   * Abstract method for service-specific schema name. Single responsibility: Define
   * service-specific schema.
   */
  protected abstract String getSchemaName();

  /**
   * Abstract method for service-specific entity classes. Single responsibility: Define
   * service-specific entities.
   */
  protected abstract List<Class<?>> getEntityClasses();

  /**
   * Abstract method for service-specific database configuration. Single responsibility: Define
   * service-specific database behavior.
   */
  protected abstract String getServiceName();
}
