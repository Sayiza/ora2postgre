package me.christianrobert.ora2postgre.plsql.ast.tools.strategies;

import me.christianrobert.ora2postgre.oracledb.IndexMetadata;
import me.christianrobert.ora2postgre.oracledb.IndexColumn;
import me.christianrobert.ora2postgre.plsql.ast.tools.transformers.PostgreSQLIndexDDL;

/**
 * Strategy for converting Oracle unique indexes to PostgreSQL unique indexes.
 * Handles both single-column and composite unique indexes.
 */
public class UniqueIndexStrategy implements IndexMigrationStrategy {

  @Override
  public boolean supports(IndexMetadata index) {
    // Support unique indexes that are not functional
    return index.isUniqueIndex() &&
            !index.isFunctional() &&
            index.isValid() &&
            !index.getColumns().isEmpty();
  }

  @Override
  public PostgreSQLIndexDDL convert(IndexMetadata index) {
    if (!supports(index)) {
      throw new UnsupportedOperationException("Index not supported by UniqueIndexStrategy: " + index.getIndexName());
    }

    StringBuilder sql = new StringBuilder();

    // Build CREATE UNIQUE INDEX statement
    sql.append("CREATE UNIQUE INDEX ");

    // Generate PostgreSQL-compatible index name (handle length limit)
    String pgIndexName = generatePostgreSQLIndexName(index);
    sql.append(pgIndexName);

    // ON clause with table name
    sql.append(" ON ");
    if (index.getSchemaName() != null) {
      sql.append(index.getSchemaName().toLowerCase()).append(".");
    }
    sql.append(index.getTableName().toLowerCase());

    // Column list
    sql.append(" (");
    for (int i = 0; i < index.getColumns().size(); i++) {
      if (i > 0) {
        sql.append(", ");
      }
      IndexColumn column = index.getColumns().get(i);
      sql.append(column.getColumnName().toLowerCase());

      // Add sort order if specified
      if (column.isDescending()) {
        sql.append(" DESC");
      }
    }
    sql.append(")");

    // Add tablespace if specified and not default
    if (index.getTablespace() != null &&
            !index.getTablespace().trim().isEmpty() &&
            !"USERS".equalsIgnoreCase(index.getTablespace()) &&
            !"SYSTEM".equalsIgnoreCase(index.getTablespace())) {
      sql.append(" TABLESPACE ").append(index.getTablespace().toLowerCase());
    }

    String conversionNotes = generateConversionNotes(index);

    return new PostgreSQLIndexDDL(
            sql.toString(),
            pgIndexName,
            index.getIndexName(),
            index.getTableName(),
            index.getSchemaName(),
            "POST_TRANSFER_INDEXES",
            conversionNotes
    );
  }

  @Override
  public String getStrategyName() {
    return "Unique Index";
  }

  @Override
  public int getPriority() {
    return 20; // Higher priority than regular B-tree indexes
  }

  /**
   * Generates a PostgreSQL-compatible index name, handling the 63-character limit.
   */
  private String generatePostgreSQLIndexName(IndexMetadata index) {
    String originalName = index.getIndexName().toLowerCase();

    // PostgreSQL identifier limit is 63 characters
    if (originalName.length() <= 63) {
      return originalName;
    }

    // Truncate and add hash to ensure uniqueness
    String baseNameTruncated = originalName.substring(0, 55);
    String hash = String.format("%08x", originalName.hashCode()).substring(0, 7);
    return baseNameTruncated + "_" + hash;
  }

  /**
   * Generates conversion notes based on the index characteristics.
   */
  private String generateConversionNotes(IndexMetadata index) {
    StringBuilder notes = new StringBuilder();

    if (index.isComposite()) {
      notes.append("Composite unique index with ").append(index.getColumns().size()).append(" columns");
    } else {
      notes.append("Single-column unique index");
    }

    // Check for descending columns
    long descendingColumns = index.getColumns().stream()
            .mapToLong(col -> col.isDescending() ? 1 : 0)
            .sum();

    if (descendingColumns > 0) {
      if (notes.length() > 0) notes.append("; ");
      notes.append(descendingColumns).append(" descending column(s)");
    }

    // Check if name was truncated
    if (index.getIndexName().length() > 63) {
      if (notes.length() > 0) notes.append("; ");
      notes.append("Name truncated due to PostgreSQL 63-char limit");
    }

    // Check for unusual tablespace
    if (index.getTablespace() != null &&
            !index.getTablespace().trim().isEmpty() &&
            !"USERS".equalsIgnoreCase(index.getTablespace()) &&
            !"SYSTEM".equalsIgnoreCase(index.getTablespace())) {
      if (notes.length() > 0) notes.append("; ");
      notes.append("Custom tablespace: ").append(index.getTablespace());
    }

    // Add uniqueness note
    if (notes.length() > 0) notes.append("; ");
    notes.append("Enforces uniqueness constraint");

    return notes.toString();
  }

  @Override
  public String getConversionNotes(IndexMetadata index) {
    return generateConversionNotes(index);
  }
}