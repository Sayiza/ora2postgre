package me.christianrobert.ora2postgre.writing;

import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PostgreSQLIndexDDL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates detailed reports for Oracle indexes that could not be automatically 
 * converted to PostgreSQL equivalents. Provides comprehensive information for 
 * manual review and implementation.
 */
public class UnsupportedIndexReporter {

  private static final Logger log = LoggerFactory.getLogger(UnsupportedIndexReporter.class);

  /**
   * Generates a comprehensive report of unsupported indexes with detailed
   * information for manual implementation.
   *
   * @param basePath Base path for report generation
   * @param unsupportedIndexes List of unsupported PostgreSQL DDL objects
   * @param conversionStats Overall conversion statistics
   */
  public static void generateUnsupportedIndexReport(String basePath,
                                                    List<PostgreSQLIndexDDL> unsupportedIndexes,
                                                    Map<String, Integer> conversionStats) {
    if (unsupportedIndexes.isEmpty()) {
      log.info("No unsupported indexes found - skipping report generation");
      return;
    }

    log.info("Generating unsupported index report for {} indexes", unsupportedIndexes.size());

    // Group by schema for better organization
    Map<String, List<PostgreSQLIndexDDL>> indexesBySchema = unsupportedIndexes.stream()
            .collect(Collectors.groupingBy(
                    index -> index.getSchemaName().toLowerCase(),
                    Collectors.toList()
            ));

    // Generate summary report
    generateSummaryReport(basePath, unsupportedIndexes, conversionStats, indexesBySchema);

    // Generate detailed per-schema reports
    for (Map.Entry<String, List<PostgreSQLIndexDDL>> entry : indexesBySchema.entrySet()) {
      generateSchemaReport(basePath, entry.getKey(), entry.getValue());
    }

    log.info("Unsupported index report generation completed for {} schemas", indexesBySchema.size());
  }

  /**
   * Generates a comprehensive summary report of all unsupported indexes.
   *
   * @param basePath Base path for report generation
   * @param unsupportedIndexes List of all unsupported indexes
   * @param conversionStats Overall conversion statistics
   * @param indexesBySchema Indexes grouped by schema
   */
  private static void generateSummaryReport(String basePath,
                                            List<PostgreSQLIndexDDL> unsupportedIndexes,
                                            Map<String, Integer> conversionStats,
                                            Map<String, List<PostgreSQLIndexDDL>> indexesBySchema) {
    StringBuilder report = new StringBuilder();

    // Header
    report.append("ORACLE TO POSTGRESQL INDEX MIGRATION REPORT\n");
    report.append("==========================================\n\n");

    // Summary statistics
    report.append("UNSUPPORTED INDEX SUMMARY\n");
    report.append("========================\n\n");

    report.append("Total unsupported indexes: ").append(unsupportedIndexes.size()).append("\n");
    report.append("Affected schemas: ").append(indexesBySchema.size()).append("\n");
    report.append("Report generated: ").append(new java.util.Date()).append("\n\n");

    // Strategy statistics
    if (!conversionStats.isEmpty()) {
      report.append("CONVERSION STRATEGY STATISTICS\n");
      report.append("=============================\n\n");

      conversionStats.entrySet().stream()
              .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
              .forEach(entry -> {
                report.append(String.format("%-25s: %d indexes\n", entry.getKey(), entry.getValue()));
              });
      report.append("\n");
    }

    // Schema breakdown
    report.append("SCHEMA BREAKDOWN\n");
    report.append("===============\n\n");

    indexesBySchema.entrySet().stream()
            .sorted(Map.Entry.<String, List<PostgreSQLIndexDDL>>comparingByKey())
            .forEach(entry -> {
              String schema = entry.getKey();
              List<PostgreSQLIndexDDL> schemaIndexes = entry.getValue();

              report.append(String.format("Schema: %s\n", schema.toUpperCase()));
              report.append(String.format("  Unsupported indexes: %d\n", schemaIndexes.size()));

              // Count by reason
              Map<String, Long> reasonCounts = schemaIndexes.stream()
                      .collect(Collectors.groupingBy(
                              index -> index.getConversionNotes(),
                              Collectors.counting()
                      ));

              reasonCounts.entrySet().stream()
                      .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                      .forEach(reasonEntry -> {
                        report.append(String.format("    %s: %d\n", reasonEntry.getKey(), reasonEntry.getValue()));
                      });

              report.append("\n");
            });

    // Common issues and recommendations
    report.append("COMMON ISSUES AND RECOMMENDATIONS\n");
    report.append("=================================\n\n");

    report.append("1. BITMAP INDEXES\n");
    report.append("   Issue: PostgreSQL does not support stored bitmap indexes\n");
    report.append("   Solution: Use regular B-tree indexes; PostgreSQL will automatically\n");
    report.append("             use bitmap scans when beneficial for low-cardinality columns\n\n");

    report.append("2. FUNCTION-BASED INDEXES\n");
    report.append("   Issue: Complex Oracle expressions may not translate directly\n");
    report.append("   Solution: Review expressions and adapt to PostgreSQL syntax\n");
    report.append("             Consider using PostgreSQL partial indexes for similar functionality\n\n");

    report.append("3. REVERSE KEY INDEXES\n");
    report.append("   Issue: PostgreSQL does not support reverse key indexes\n");
    report.append("   Solution: Use regular B-tree indexes; consider hash indexes for equality\n");
    report.append("             lookups if appropriate\n\n");

    report.append("4. DOMAIN INDEXES\n");
    report.append("   Issue: Oracle-specific index types (text, spatial, etc.)\n");
    report.append("   Solution: Use PostgreSQL extensions (full-text search, PostGIS) or\n");
    report.append("             equivalent PostgreSQL functionality\n\n");

    report.append("NEXT STEPS\n");
    report.append("==========\n\n");
    report.append("1. Review detailed per-schema reports in reports/ directory\n");
    report.append("2. Analyze each unsupported index for migration strategy\n");
    report.append("3. Implement PostgreSQL equivalents manually\n");
    report.append("4. Test index performance in PostgreSQL environment\n");
    report.append("5. Update application queries if necessary\n\n");

    // Write summary report
    String reportPath = basePath + File.separator + "reports";
    FileWriter.write(Paths.get(reportPath), "unsupported_indexes_summary.txt", report.toString());

    log.info("Generated unsupported index summary report");
  }

  /**
   * Generates a detailed report for unsupported indexes in a specific schema.
   *
   * @param basePath Base path for report generation
   * @param schema Schema name
   * @param schemaIndexes List of unsupported indexes in this schema
   */
  private static void generateSchemaReport(String basePath, String schema, List<PostgreSQLIndexDDL> schemaIndexes) {
    StringBuilder report = new StringBuilder();

    // Header
    report.append("UNSUPPORTED INDEX REPORT FOR SCHEMA: ").append(schema.toUpperCase()).append("\n");
    report.append("=".repeat(50 + schema.length())).append("\n\n");

    report.append("Schema: ").append(schema.toUpperCase()).append("\n");
    report.append("Unsupported indexes: ").append(schemaIndexes.size()).append("\n");
    report.append("Report generated: ").append(new java.util.Date()).append("\n\n");

    // Sort by table name and index name for better organization
    schemaIndexes.sort((a, b) -> {
      int tableComparison = a.getTableName().compareToIgnoreCase(b.getTableName());
      return tableComparison != 0 ? tableComparison : a.getOriginalIndexName().compareToIgnoreCase(b.getOriginalIndexName());
    });

    // Detailed index information
    report.append("DETAILED INDEX INFORMATION\n");
    report.append("=========================\n\n");

    for (PostgreSQLIndexDDL index : schemaIndexes) {
      report.append("Index: ").append(index.getOriginalIndexName()).append("\n");
      report.append("-".repeat(index.getOriginalIndexName().length() + 7)).append("\n");

      report.append("Table: ").append(index.getFullTableName()).append("\n");
      report.append("Reason: ").append(index.getConversionNotes()).append("\n");

      // Add specific recommendations based on the reason
      String recommendations = generateRecommendations(index.getConversionNotes());
      if (!recommendations.isEmpty()) {
        report.append("Recommendations:\n");
        report.append(recommendations).append("\n");
      }

      report.append("\n");
    }

    // Summary by reason
    report.append("SUMMARY BY REASON\n");
    report.append("=================\n\n");

    Map<String, Long> reasonCounts = schemaIndexes.stream()
            .collect(Collectors.groupingBy(
                    index -> index.getConversionNotes(),
                    Collectors.counting()
            ));

    reasonCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> {
              report.append(String.format("%-40s: %d indexes\n", entry.getKey(), entry.getValue()));
            });

    // Write schema-specific report
    String reportPath = basePath + File.separator + "reports";
    String fileName = schema.toLowerCase() + "_unsupported_indexes.txt";
    FileWriter.write(Paths.get(reportPath), fileName, report.toString());

    log.info("Generated detailed unsupported index report for schema: {}", schema);
  }

  /**
   * Generates specific recommendations based on the conversion issue.
   *
   * @param conversionNotes The reason why the index was not converted
   * @return Specific recommendations for manual implementation
   */
  private static String generateRecommendations(String conversionNotes) {
    if (conversionNotes == null || conversionNotes.trim().isEmpty()) {
      return "";
    }

    String notes = conversionNotes.toLowerCase();
    StringBuilder recommendations = new StringBuilder();

    if (notes.contains("bitmap")) {
      recommendations.append("  • Replace with regular B-tree index\n");
      recommendations.append("  • PostgreSQL will automatically use bitmap scans when beneficial\n");
      recommendations.append("  • Consider partial indexes for better selectivity\n");
    }

    if (notes.contains("function") || notes.contains("expression")) {
      recommendations.append("  • Review the function expression for PostgreSQL compatibility\n");
      recommendations.append("  • Convert Oracle functions to PostgreSQL equivalents\n");
      recommendations.append("  • Consider creating a PostgreSQL function if needed\n");
    }

    if (notes.contains("reverse")) {
      recommendations.append("  • Use regular B-tree index instead\n");
      recommendations.append("  • Consider hash index for equality lookups only\n");
      recommendations.append("  • Evaluate if reverse key behavior is actually needed\n");
    }

    if (notes.contains("domain")) {
      recommendations.append("  • Identify the specific Oracle domain index type\n");
      recommendations.append("  • Use appropriate PostgreSQL extension (full-text, PostGIS, etc.)\n");
      recommendations.append("  • Consider alternative PostgreSQL indexing strategies\n");
    }

    if (notes.contains("partition")) {
      recommendations.append("  • PostgreSQL supports partitioned tables with local indexes\n");
      recommendations.append("  • Create equivalent index on each partition\n");
      recommendations.append("  • Consider PostgreSQL declarative partitioning\n");
    }

    if (notes.contains("invalid") || notes.contains("unusable")) {
      recommendations.append("  • Verify if the index is actually needed\n");
      recommendations.append("  • Check if the underlying issue has been resolved\n");
      recommendations.append("  • Recreate the index if it's still required\n");
    }

    if (recommendations.length() == 0) {
      recommendations.append("  • Manual analysis required for this specific case\n");
      recommendations.append("  • Consult PostgreSQL documentation for equivalent functionality\n");
    }

    return recommendations.toString();
  }

  /**
   * Generates a simple text report for unsupported indexes.
   * Used when full reporting is not needed.
   *
   * @param unsupportedIndexes List of unsupported indexes
   * @return Simple text report
   */
  public static String generateSimpleReport(List<PostgreSQLIndexDDL> unsupportedIndexes) {
    if (unsupportedIndexes.isEmpty()) {
      return "No unsupported indexes found.";
    }

    StringBuilder report = new StringBuilder();
    report.append("Unsupported Indexes (").append(unsupportedIndexes.size()).append(" total):\n");

    for (PostgreSQLIndexDDL index : unsupportedIndexes) {
      report.append("  • ").append(index.getOriginalIndexName())
              .append(" (").append(index.getFullTableName()).append(")")
              .append(" - ").append(index.getConversionNotes())
              .append("\n");
    }

    return report.toString();
  }
}