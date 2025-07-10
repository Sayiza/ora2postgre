package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.oracledb.IndexColumn;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PostgreSQLIndexDDL;

/**
 * Strategy for handling Oracle indexes that cannot be automatically converted to PostgreSQL.
 * This strategy generates detailed reports about unsupported indexes for manual review.
 */
public class UnsupportedIndexStrategy implements IndexMigrationStrategy {

  @Override
  public boolean supports(IndexMetadata index) {
    // This strategy handles any index that other strategies don't support
    // It acts as a catch-all for problematic conversions
    return true; // Always supports, but with lowest priority
  }

  @Override
  public PostgreSQLIndexDDL convert(IndexMetadata index) {
    String reason = determineUnsupportedReason(index);

    return new PostgreSQLIndexDDL(
            index.getIndexName(),
            index.getTableName(),
            index.getSchemaName(),
            reason
    );
  }

  @Override
  public String getStrategyName() {
    return "Unsupported Index";
  }

  @Override
  public int getPriority() {
    return -1; // Lowest priority - only used when no other strategy matches
  }

  @Override
  public boolean generatesDDL() {
    return false; // This strategy only generates reports, not executable DDL
  }

  /**
   * Determines the specific reason why an index is unsupported.
   */
  private String determineUnsupportedReason(IndexMetadata index) {
    StringBuilder reason = new StringBuilder();

    // Check for bitmap indexes
    if (index.isBitmap()) {
      reason.append("Bitmap indexes are not supported in PostgreSQL. ");
      reason.append("PostgreSQL uses bitmap scans automatically when beneficial. ");
      reason.append("Consider creating a regular B-tree index instead.");
      return reason.toString();
    }

    // Check for function-based indexes with complex expressions
    if (index.isFunctional()) {
      reason.append("Function-based index with complex expressions requires manual conversion. ");

      // Analyze the expressions
      boolean hasComplexExpressions = index.getColumns().stream()
              .anyMatch(IndexColumn::hasComplexExpression);

      if (hasComplexExpressions) {
        reason.append("Contains Oracle-specific functions that need transformation. ");
      }

      reason.append("Review expressions and create equivalent PostgreSQL functional index manually.");
      return reason.toString();
    }

    // Check for invalid status
    if (!index.isValid()) {
      reason.append("Index status is ").append(index.getStatus()).append(" (not VALID). ");
      reason.append("Only valid indexes can be migrated. ");
      reason.append("Rebuild the index in Oracle before migration.");
      return reason.toString();
    }

    // Check for empty columns
    if (index.getColumns().isEmpty()) {
      reason.append("Index has no columns defined. ");
      reason.append("This may indicate corrupted metadata or system-generated index. ");
      reason.append("Review index definition in Oracle.");
      return reason.toString();
    }

    // Check for reverse key indexes
    if ("NORMAL/REV".equalsIgnoreCase(index.getIndexType())) {
      reason.append("Reverse key indexes are Oracle-specific optimization. ");
      reason.append("PostgreSQL doesn't have equivalent feature. ");
      reason.append("Consider creating regular B-tree index instead.");
      return reason.toString();
    }

    // Check for domain indexes
    if ("DOMAIN".equalsIgnoreCase(index.getIndexType())) {
      reason.append("Domain indexes are Oracle-specific extensible indexing feature. ");
      reason.append("PostgreSQL doesn't have direct equivalent. ");
      reason.append("Review index usage and implement alternative solution.");
      return reason.toString();
    }

    // Check for cluster indexes
    if ("CLUSTER".equalsIgnoreCase(index.getIndexType())) {
      reason.append("Cluster indexes work differently in PostgreSQL. ");
      reason.append("Use PostgreSQL CLUSTER command on table with appropriate index. ");
      reason.append("Create regular index first, then cluster table on it.");
      return reason.toString();
    }

    // Check for partitioned indexes with complex partitioning
    if (index.isPartitioned()) {
      reason.append("Partitioned index may require special handling. ");
      reason.append("PostgreSQL partitioned indexes work differently than Oracle. ");
      reason.append("Review partitioning strategy and create appropriate indexes on partitions.");
      return reason.toString();
    }

    // Check for very long names that would be problematic
    if (index.getIndexName().length() > 63) {
      reason.append("Index name exceeds PostgreSQL 63-character limit. ");
      reason.append("Automatic truncation may cause conflicts. ");
      reason.append("Choose shorter name manually.");
      return reason.toString();
    }

    // Check for indexes with too many columns (PostgreSQL limit is 32)
    if (index.getColumns().size() > 32) {
      reason.append("Index has ").append(index.getColumns().size()).append(" columns. ");
      reason.append("PostgreSQL supports maximum 32 columns per index. ");
      reason.append("Split into multiple indexes or reduce column count.");
      return reason.toString();
    }

    // Generic fallback reason
    reason.append("Index type '").append(index.getIndexType()).append("' ");
    reason.append("requires manual review for PostgreSQL conversion. ");
    reason.append("Check Oracle documentation for index characteristics ");
    reason.append("and implement equivalent PostgreSQL solution.");

    return reason.toString();
  }

  /**
   * Generates a detailed report entry for the unsupported index.
   */
  public String generateReportEntry(IndexMetadata index) {
    StringBuilder report = new StringBuilder();

    report.append("UNSUPPORTED INDEX: ").append(index.getFullName()).append("\n");
    report.append("Table: ").append(index.getFullTableName()).append("\n");
    report.append("Type: ").append(index.getIndexType()).append("\n");
    report.append("Status: ").append(index.getStatus()).append("\n");

    if (index.isUniqueIndex()) {
      report.append("Uniqueness: UNIQUE\n");
    }

    if (index.isPartitioned()) {
      report.append("Partitioned: YES\n");
    }

    if (index.getTablespace() != null) {
      report.append("Tablespace: ").append(index.getTablespace()).append("\n");
    }

    report.append("Columns (").append(index.getColumns().size()).append("): ");
    for (int i = 0; i < index.getColumns().size(); i++) {
      if (i > 0) report.append(", ");
      IndexColumn col = index.getColumns().get(i);
      report.append(col.getColumnName());
      if (col.isFunctional()) {
        report.append(" (EXPR: ").append(col.getColumnExpression()).append(")");
      }
      if (col.isDescending()) {
        report.append(" DESC");
      }
    }
    report.append("\n");

    report.append("Reason: ").append(determineUnsupportedReason(index)).append("\n");

    // Suggest alternatives
    report.append("Suggested Action: ");
    if (index.isBitmap()) {
      report.append("Create B-tree index: CREATE INDEX ").append(index.getIndexName().toLowerCase())
              .append(" ON ").append(index.getFullTableName()).append(" (");
      for (int i = 0; i < index.getColumns().size(); i++) {
        if (i > 0) report.append(", ");
        report.append(index.getColumns().get(i).getColumnName().toLowerCase());
      }
      report.append(");");
    } else if (index.isFunctional()) {
      report.append("Review expressions and create functional index with PostgreSQL-compatible functions");
    } else {
      report.append("Review index requirements and create appropriate PostgreSQL equivalent");
    }
    report.append("\n");

    report.append("---\n");

    return report.toString();
  }

  @Override
  public String getConversionNotes(IndexMetadata index) {
    return determineUnsupportedReason(index);
  }
}