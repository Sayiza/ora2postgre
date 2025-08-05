package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.IndexMigrationStrategyManager;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.IndexMigrationStrategyManager.IndexConversionResult;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PostgreSQLIndexDDL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Export utility for generating PostgreSQL index files.
 *
 * Implements simplified PostgreSQL-compatible index export strategy:
 * 1. Convert Oracle indexes using strategy pattern with constraint deduplication
 * 2. Generate PostgreSQL CREATE INDEX statements
 * 3. Organize indexes by schema in step6indexes/ directory
 * 4. Use standard logging like other export classes
 *
 * Indexes are created after data transfer for optimal performance.
 */
public class ExportIndex {

  private static final Logger log = LoggerFactory.getLogger(ExportIndex.class);
  private static final IndexMigrationStrategyManager strategyManager = new IndexMigrationStrategyManager();

  /**
   * Main entry point for index export. Converts Oracle indexes to PostgreSQL
   * and generates files in the correct directory structure.
   *
   * @param basePath Base path for file generation (e.g., target-project/postgre/autoddl/)
   * @param everything Global context containing all index data
   */
  public static void saveIndexes(String basePath, Everything everything) {
    log.info("Starting index export to base path: {}", basePath);

    List<IndexMetadata> indexes = everything.getIndexes();

    if (indexes.isEmpty()) {
      log.info("No indexes found - skipping index export");
      return;
    }

    log.info("Exporting {} indexes", indexes.size());

    // Convert all indexes using strategy manager with constraint deduplication
    IndexConversionResult conversionResult = strategyManager.convertIndexes(indexes, everything);

    // Export supported indexes
    exportSupportedIndexes(basePath, conversionResult.getSupportedIndexes());

    // Log unsupported indexes (simple logging, no complex reporting)
    logUnsupportedIndexes(conversionResult.getUnsupportedIndexes());

    // Log conversion statistics
    logConversionStatistics(conversionResult);

    log.info("Index export completed successfully");
  }

  /**
   * Exports supported indexes to schema-specific SQL files.
   *
   * @param basePath Base path for file generation
   * @param supportedIndexes List of PostgreSQL DDL objects for supported indexes
   */
  private static void exportSupportedIndexes(String basePath, List<PostgreSQLIndexDDL> supportedIndexes) {
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

    Map<String, Integer> schemaCounts = new HashMap<>();

    // Export each schema's indexes to a separate file
    for (Map.Entry<String, List<PostgreSQLIndexDDL>> entry : indexesBySchema.entrySet()) {
      String schema = entry.getKey();
      List<PostgreSQLIndexDDL> schemaIndexes = entry.getValue();

      exportSchemaIndexes(basePath, schema, schemaIndexes);
      schemaCounts.put(schema, schemaIndexes.size());
    }

    // Log export statistics
    int totalIndexes = schemaCounts.values().stream().mapToInt(Integer::intValue).sum();
    log.info("Exported {} indexes across {} schemas", totalIndexes, schemaCounts.size());
    for (Map.Entry<String, Integer> entry : schemaCounts.entrySet()) {
      log.debug("  Schema {}: {} indexes", entry.getKey(), entry.getValue());
    }
  }

  /**
   * Exports indexes for a specific schema to a SQL file.
   *
   * @param basePath Base path for file generation
   * @param schema Schema name
   * @param schemaIndexes List of indexes for this schema
   */
  private static void exportSchemaIndexes(String basePath, String schema, List<PostgreSQLIndexDDL> schemaIndexes) {
    StringBuilder content = new StringBuilder();

    // Add file header
    content.append("-- PostgreSQL Index Definitions for Schema: ").append(schema.toUpperCase()).append("\n");
    content.append("-- Generated from Oracle index migration with constraint deduplication\n");
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

    log.debug("Exported {} indexes for schema {} to {}", schemaIndexes.size(), schema, fileName);
  }

  /**
   * Logs unsupported indexes using standard logging (no complex reporting).
   *
   * @param unsupportedIndexes List of unsupported index DDL objects
   */
  private static void logUnsupportedIndexes(List<PostgreSQLIndexDDL> unsupportedIndexes) {
    if (unsupportedIndexes.isEmpty()) {
      return;
    }

    log.warn("Found {} unsupported indexes that require manual review:", unsupportedIndexes.size());
    
    for (PostgreSQLIndexDDL index : unsupportedIndexes) {
      log.warn("  • {} (Table: {}) - {}", 
          index.getOriginalIndexName(), 
          index.getFullTableName(), 
          index.getConversionNotes());
    }
  }

  /**
   * Gets the full directory path for index files.
   *
   * @param basePath Base path for file generation
   * @param schema Schema name
   * @return Full directory path for index files
   */
  private static String getIndexPath(String basePath, String schema) {
    return basePath + File.separator + schema.toLowerCase() + File.separator + "step6indexes";
  }

  /**
   * Logs detailed conversion statistics.
   *
   * @param conversionResult Result from index conversion process
   */
  private static void logConversionStatistics(IndexConversionResult conversionResult) {
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