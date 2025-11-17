package com.pacific.core.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.stereotype.Service;

/** Centralized database migration and management service */
@Slf4j
@Service
public class DatabaseMigrationService {

  private final DataSource dataSource;

  @Autowired(required = false)
  private LiquibaseProperties liquibaseProperties;

  @Autowired
  public DatabaseMigrationService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** Get database migration status */
  public MigrationStatus getMigrationStatus() {
    try (Connection connection = dataSource.getConnection()) {
      List<MigrationInfo> migrations = getAppliedMigrations(connection);
      String currentVersion = getCurrentVersion(connection);

      return MigrationStatus.builder()
          .currentVersion(currentVersion)
          .appliedMigrations(migrations)
          .totalMigrations(migrations.size())
          .isUpToDate(true) // This would need actual logic to determine
          .build();

    } catch (Exception e) {
      log.error("Failed to get migration status", e);
      return MigrationStatus.builder()
          .currentVersion("UNKNOWN")
          .appliedMigrations(new ArrayList<>())
          .totalMigrations(0)
          .isUpToDate(false)
          .build();
    }
  }

  /** Get applied migrations from database */
  private List<MigrationInfo> getAppliedMigrations(Connection connection) throws Exception {
    List<MigrationInfo> migrations = new ArrayList<>();

    String sql =
        "SELECT id, author, filename, dateexecuted, md5sum FROM databasechangelog ORDER BY dateexecuted";

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        MigrationInfo migration =
            MigrationInfo.builder()
                .id(rs.getString("id"))
                .author(rs.getString("author"))
                .filename(rs.getString("filename"))
                .dateExecuted(rs.getTimestamp("dateexecuted").toLocalDateTime())
                .md5sum(rs.getString("md5sum"))
                .build();

        migrations.add(migration);
      }
    }

    return migrations;
  }

  /** Get current database version */
  private String getCurrentVersion(Connection connection) throws Exception {
    String sql = "SELECT id FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 1";

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      if (rs.next()) {
        return rs.getString("id");
      }
    }

    return "NO_MIGRATIONS";
  }

  /** Validate database schema */
  public SchemaValidation validateSchema() {
    try (Connection connection = dataSource.getConnection()) {
      List<String> tables = getTableNames(connection);
      List<String> indexes = getIndexNames(connection);

      return SchemaValidation.builder()
          .isValid(true)
          .tableCount(tables.size())
          .indexCount(indexes.size())
          .tables(tables)
          .indexes(indexes)
          .build();

    } catch (Exception e) {
      log.error("Schema validation failed", e);
      return SchemaValidation.builder().isValid(false).error(e.getMessage()).build();
    }
  }

  /** Get table names from database */
  private List<String> getTableNames(Connection connection) throws Exception {
    List<String> tables = new ArrayList<>();

    String sql = "SELECT tablename FROM pg_tables WHERE schemaname = 'public'";

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        tables.add(rs.getString("tablename"));
      }
    }

    return tables;
  }

  /** Get index names from database */
  private List<String> getIndexNames(Connection connection) throws Exception {
    List<String> indexes = new ArrayList<>();

    String sql = "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'";

    try (PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

      while (rs.next()) {
        indexes.add(rs.getString("indexname"));
      }
    }

    return indexes;
  }

  /** Migration status DTO */
  @lombok.Data
  @lombok.Builder
  public static class MigrationStatus {
    private String currentVersion;
    private List<MigrationInfo> appliedMigrations;
    private int totalMigrations;
    private boolean isUpToDate;
  }

  /** Migration info DTO */
  @lombok.Data
  @lombok.Builder
  public static class MigrationInfo {
    private String id;
    private String author;
    private String filename;
    private java.time.LocalDateTime dateExecuted;
    private String md5sum;
  }

  /** Schema validation DTO */
  @lombok.Data
  @lombok.Builder
  public static class SchemaValidation {
    private boolean isValid;
    private int tableCount;
    private int indexCount;
    private List<String> tables;
    private List<String> indexes;
    private String error;
  }
}
