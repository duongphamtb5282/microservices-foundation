package com.pacific.customer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * Simple MongoDB migration runner. Executes database migrations in order on application startup.
 */
@Slf4j
@RequiredArgsConstructor
public class MongoMigrationRunner {

  private final MongoTemplate mongoTemplate;

  /** Runs all database migrations in order. */
  public void runMigrations() {
    log.info("🔧 Starting MongoDB migrations");

    // Run migrations in order
    runMigration("001_create_indexes", this::createIndexes);
    runMigration("002_create_sample_data", this::createSampleData);

    log.info("✅ All migrations completed");
  }

  /** Runs a single migration. */
  private void runMigration(String migrationId, Runnable migration) {
    log.info("🚀 Executing migration: {}", migrationId);
    try {
      migration.run();
      log.info("✅ Migration '{}' completed successfully", migrationId);
    } catch (Exception e) {
      log.error("❌ Migration '{}' failed: {}", migrationId, e.getMessage(), e);
      throw new RuntimeException("Migration failed: " + migrationId, e);
    }
  }

  /** Migration 001: Create indexes for customers collection. */
  private void createIndexes() {
    // Create unique index on email
    mongoTemplate
        .indexOps("customers")
        .ensureIndex(
            new Index().on("email", org.springframework.data.domain.Sort.Direction.ASC).unique());

    // Create index on status
    mongoTemplate
        .indexOps("customers")
        .ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));

    // Create index on createdAt
    mongoTemplate
        .indexOps("customers")
        .ensureIndex(
            new Index().on("createdAt", org.springframework.data.domain.Sort.Direction.ASC));

    log.info("✅ Created indexes for customers collection");
  }

  /** Migration 002: Create sample customer data. */
  private void createSampleData() {
    // Only create sample data if collection is empty
    if (mongoTemplate.count(new org.springframework.data.mongodb.core.query.Query(), "customers")
        == 0) {
      // Sample data would be inserted here if needed
      log.info("ℹ️  No sample data needed - collection is empty");
    } else {
      log.info("ℹ️  Sample data skipped - collection already has data");
    }
  }
}
