package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.plsql.ast.tools.IndexMigrationStrategyManager;
import me.christianrobert.ora2postgre.plsql.ast.tools.IndexMigrationStrategyManager.IndexConversionResult;
import me.christianrobert.ora2postgre.plsql.ast.tools.PostgreSQLIndexDDL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Export utility for generating PostgreSQL index files.
 *
 * Implements PostgreSQL-compatible index export strategy:
 * 1. Convert Oracle indexes using strategy pattern
 * 2. Generate PostgreSQL CREATE INDEX statements
 * 3. Organize indexes by schema in step6indexes/ directory
 * 4. Handle supported/unsupported indexes appropriately
 *
 * Indexes are created after data transfer for optimal performance.
 */
public class ExportIndex {

  private static final Logger log = LoggerFactory.getLogger(ExportIndex.class);

  private final IndexMigrationStrategyManager strategyManager;

  /**
   * Constructor that initializes the strategy manager.
   */
  public ExportIndex() {
    this.strategyManager = new IndexMigrationStrategyManager();
  }

  /**
   * Main entry point for index export. Converts Oracle indexes to PostgreSQL
   * and generates files in the correct directory structure.
   *
   * @param basePath Base path for file generation (e.g., target-project/postgre/autoddl/)
   * @param everything Global context containing all index data
   */
  public static void saveIndexes(String basePath, Everything everything) {
    log.info("Starting index export to base path: {}", basePath);

    ExportIndex exporter = new ExportIndex();
    exporter.exportIndexes(basePath, everything);

    log.info("Index export completed successfully");
  }

  /**
   * Exports all indexes from the Everything context.
   *
   * @param basePath Base path for file generation
   * @param everything Global context containing index data
   */
  public void exportIndexes(String basePath, Everything everything) {
    List<IndexMetadata> indexes = everything.getIndexes();

    if (indexes.isEmpty()) {
      log.info("No indexes found - skipping index export");
      return;
    }

    log.info("Exporting {} indexes", indexes.size());

    // Convert all indexes using strategy manager
    IndexConversionResult conversionResult = strategyManager.convertIndexes(indexes);

    // Export supported indexes
    exportSupportedIndexes(basePath, conversionResult.getSupportedIndexes());

    // Export unsupported indexes (as comments for reference)
    exportUnsupportedIndexes(basePath, conversionResult.getUnsupportedIndexes());

    // Generate detailed unsupported index report
    if (!conversionResult.getUnsupportedIndexes().isEmpty()) {
      UnsupportedIndexReporter.generateUnsupportedIndexReport(
              basePath,
              conversionResult.getUnsupportedIndexes(),
              conversionResult.getStrategyUsageStats()
      );
    }

    // Log conversion statistics
    logConversionStatistics(conversionResult);
  }

  /**
   * Exports supported indexes to schema-specific SQL files.
   *
   * @param basePath Base path for file generation
   * @param supportedIndexes List of PostgreSQL DDL objects for supported indexes
   */
  private void exportSupportedIndexes(String basePath, List<PostgreSQLIndexDDL> supportedIndexes) {
    if (supportedIndexes.isEmpty()) {
      log.info("No supported indexes to export");
      return;
    }

    log.info("Exporting {} supported indexes", supportedIndexes.size());

    // Group indexes by schema
    Map<String, List<PostgreSQLIndexDDL>> indexesBySchema = supportedIndexes.stream()
            .collect(Collectors.groupingBy(
                    index -> index.getSchemaName().toLowerCase(),
                    Collectors.toList()
            ));

    // Export each schema's indexes to a separate file
    for (Map.Entry<String, List<PostgreSQLIndexDDL>> entry : indexesBySchema.entrySet()) {
      String schema = entry.getKey();
      List<PostgreSQLIndexDDL> schemaIndexes = entry.getValue();

      exportSchemaIndexes(basePath, schema, schemaIndexes);
    }

    log.info("Exported supported indexes to {} schema files", indexesBySchema.size());
  }

  /**
   * Exports indexes for a specific schema to a SQL file.
   *
   * @param basePath Base path for file generation
   * @param schema Schema name
   * @param schemaIndexes List of indexes for this schema
   */
  private void exportSchemaIndexes(String basePath, String schema, List<PostgreSQLIndexDDL> schemaIndexes) {
    StringBuilder content = new StringBuilder();

    // Add file header
    content.append("-- PostgreSQL Index Definitions for Schema: ").append(schema.toUpperCase()).append("\n");
    content.append("-- Generated from Oracle index migration\n");
    content.append("-- Total indexes: ").append(schemaIndexes.size()).append("\n");
    content.append("-- Execution phase: POST_TRANSFER_INDEXES\n");
    content.append("\n");

    // Sort indexes by table name for better organization
    schemaIndexes.sort((a, b) -> {
      int tableComparison = a.getTableName().compareToIgnoreCase(b.getTableName());
      return tableComparison != 0 ? tableComparison : a.getIndexName().compareToIgnoreCase(b.getIndexName());
    });

    // Add each index DDL
    for (PostgreSQLIndexDDL index : schemaIndexes) {
      content.append(index.getFormattedSQL()).append("\n\n");
    }

    // Write to file
    String fileName = schema.toLowerCase() + "_indexes.sql";
    String directoryPath = getIndexPath(basePath, schema);

    FileWriter.write(Paths.get(directoryPath), fileName, content.toString());

    log.info("Exported {} indexes for schema {} to {}", schemaIndexes.size(), schema, fileName);
  }

  /**
   * Exports unsupported indexes as commented SQL for reference.
   *
   * @param basePath Base path for file generation
   * @param unsupportedIndexes List of unsupported index DDL objects
   */
  private void exportUnsupportedIndexes(String basePath, List<PostgreSQLIndexDDL> unsupportedIndexes) {
    if (unsupportedIndexes.isEmpty()) {
      log.info("No unsupported indexes to export");
      return;
    }

    log.info("Exporting {} unsupported indexes as comments", unsupportedIndexes.size());

    // Group unsupported indexes by schema
    Map<String, List<PostgreSQLIndexDDL>> indexesBySchema = unsupportedIndexes.stream()
            .collect(Collectors.groupingBy(
                    index -> index.getSchemaName().toLowerCase(),
                    Collectors.toList()
            ));

    // Export each schema's unsupported indexes to a separate file
    for (Map.Entry<String, List<PostgreSQLIndexDDL>> entry : indexesBySchema.entrySet()) {
      String schema = entry.getKey();
      List<PostgreSQLIndexDDL> schemaIndexes = entry.getValue();

      exportSchemaUnsupportedIndexes(basePath, schema, schemaIndexes);
    }

    log.info("Exported unsupported indexes to {} schema files", indexesBySchema.size());
  }

  /**
   * Exports unsupported indexes for a specific schema to a reference file.
   *
   * @param basePath Base path for file generation
   * @param schema Schema name
   * @param schemaIndexes List of unsupported indexes for this schema
   */
  private void exportSchemaUnsupportedIndexes(String basePath, String schema, List<PostgreSQLIndexDDL> schemaIndexes) {
    StringBuilder content = new StringBuilder();

    // Add file header
    content.append("-- UNSUPPORTED INDEX REFERENCE for Schema: ").append(schema.toUpperCase()).append("\n");
    content.append("-- These indexes could not be automatically converted\n");
    content.append("-- Manual review and implementation required\n");
    content.append("-- Total unsupported indexes: ").append(schemaIndexes.size()).append("\n");
    content.append("\n");

    // Sort indexes by table name for better organization
    schemaIndexes.sort((a, b) -> {
      int tableComparison = a.getTableName().compareToIgnoreCase(b.getTableName());
      return tableComparison != 0 ? tableComparison : a.getOriginalIndexName().compareToIgnoreCase(b.getOriginalIndexName());
    });

    // Add each unsupported index as a comment
    for (PostgreSQLIndexDDL index : schemaIndexes) {
      content.append("-- ").append(index.getSummary()).append("\n");
      content.append("-- Table: ").append(index.getFullTableName()).append("\n");
      content.append("-- Reason: ").append(index.getConversionNotes()).append("\n");
      content.append("\n");
    }

    // Write to file
    String fileName = schema.toLowerCase() + "_unsupported_indexes.sql";
    String directoryPath = getIndexPath(basePath, schema);

    FileWriter.write(Paths.get(directoryPath), fileName, content.toString());

    log.info("Exported {} unsupported indexes for schema {} to {}", schemaIndexes.size(), schema, fileName);
  }

  /**
   * Gets the full directory path for index files.
   *
   * @param basePath Base path for file generation
   * @param schema Schema name
   * @return Full directory path for index files
   */
  private String getIndexPath(String basePath, String schema) {
    return basePath + File.separator + schema.toLowerCase() + File.separator + "step6indexes";
  }

  /**
   * Logs detailed conversion statistics.
   *
   * @param conversionResult Result from index conversion process
   */
  private void logConversionStatistics(IndexConversionResult conversionResult) {
    int supportedCount = conversionResult.getSupportedIndexes().size();
    int unsupportedCount = conversionResult.getUnsupportedIndexes().size();
    int totalCount = supportedCount + unsupportedCount;

    log.info("Index conversion statistics:");
    log.info("  Total indexes processed: {}", totalCount);
    log.info("  Supported indexes: {} ({:.1f}%)", supportedCount,
            totalCount > 0 ? (supportedCount * 100.0 / totalCount) : 0.0);
    log.info("  Unsupported indexes: {} ({:.1f}%)", unsupportedCount,
            totalCount > 0 ? (unsupportedCount * 100.0 / totalCount) : 0.0);

    // Log strategy usage statistics
    Map<String, Integer> strategyStats = conversionResult.getStrategyUsageStats();
    if (!strategyStats.isEmpty()) {
      log.info("  Strategy usage:");
      for (Map.Entry<String, Integer> entry : strategyStats.entrySet()) {
        log.info("    {}: {} indexes", entry.getKey(), entry.getValue());
      }
    }
  }
}