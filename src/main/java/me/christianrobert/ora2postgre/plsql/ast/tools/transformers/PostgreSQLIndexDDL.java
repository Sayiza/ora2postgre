package me.christianrobert.ora2postgre.plsql.ast.tools.transformers;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a PostgreSQL index DDL statement generated from Oracle index metadata.
 * Contains the SQL statement and metadata for execution planning.
 */
public class PostgreSQLIndexDDL {
  private final String createIndexSQL;
  private final String indexName;
  private final String originalIndexName;
  private final String tableName;
  private final String schemaName;
  private final List<String> dependencies;
  private final String executionPhase;
  private final String conversionNotes;
  private final boolean isSupported;

  /**
   * Constructor for supported index conversions.
   */
  public PostgreSQLIndexDDL(String createIndexSQL, String indexName, String originalIndexName,
                            String tableName, String schemaName, String executionPhase, String conversionNotes) {
    this.createIndexSQL = createIndexSQL;
    this.indexName = indexName;
    this.originalIndexName = originalIndexName;
    this.tableName = tableName;
    this.schemaName = schemaName;
    this.dependencies = new ArrayList<>();
    this.executionPhase = executionPhase != null ? executionPhase : "POST_TRANSFER_INDEXES";
    this.conversionNotes = conversionNotes;
    this.isSupported = true;

    // Add table dependency
    if (schemaName != null && tableName != null) {
      this.dependencies.add(schemaName.toLowerCase() + "." + tableName.toLowerCase());
    }
  }

  /**
   * Constructor for unsupported index conversions (generates report entry instead of DDL).
   */
  public PostgreSQLIndexDDL(String originalIndexName, String tableName, String schemaName,
                            String unsupportedReason) {
    this.createIndexSQL = null;
    this.indexName = null;
    this.originalIndexName = originalIndexName;
    this.tableName = tableName;
    this.schemaName = schemaName;
    this.dependencies = new ArrayList<>();
    this.executionPhase = null;
    this.conversionNotes = unsupportedReason;
    this.isSupported = false;
  }

  // Getters
  public String getCreateIndexSQL() {
    return createIndexSQL;
  }

  public String getIndexName() {
    return indexName;
  }

  public String getOriginalIndexName() {
    return originalIndexName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public List<String> getDependencies() {
    return new ArrayList<>(dependencies);
  }

  public String getExecutionPhase() {
    return executionPhase;
  }

  public String getConversionNotes() {
    return conversionNotes;
  }

  public boolean isSupported() {
    return isSupported;
  }

  // Utility methods
  public void addDependency(String dependency) {
    if (dependency != null && !dependencies.contains(dependency)) {
      dependencies.add(dependency);
    }
  }

  public String getFullTableName() {
    return schemaName != null ? schemaName.toLowerCase() + "." + tableName.toLowerCase() : tableName.toLowerCase();
  }

  public String getFullIndexName() {
    return schemaName != null ? schemaName.toLowerCase() + "." + indexName : indexName;
  }

  /**
   * Determines if this DDL is ready for execution.
   * Supported indexes with valid SQL are ready, unsupported ones are not.
   */
  public boolean isExecutable() {
    return isSupported && createIndexSQL != null && !createIndexSQL.trim().isEmpty();
  }

  /**
   * Gets a summary line for reporting purposes.
   */
  public String getSummary() {
    if (isSupported) {
      return String.format("✓ %s → %s (%s)", originalIndexName, indexName, "Converted");
    } else {
      return String.format("✗ %s (%s)", originalIndexName, conversionNotes);
    }
  }

  /**
   * Gets the SQL statement with proper formatting for file output.
   */
  public String getFormattedSQL() {
    if (!isExecutable()) {
      return "-- Index not converted: " + originalIndexName + " (" + conversionNotes + ")";
    }

    StringBuilder sql = new StringBuilder();

    // Add header comment
    sql.append("-- Index: ").append(originalIndexName);
    if (conversionNotes != null && !conversionNotes.trim().isEmpty()) {
      sql.append(" (").append(conversionNotes).append(")");
    }
    sql.append("\n");

    // Add the DDL
    sql.append(createIndexSQL);

    // Ensure it ends with semicolon
    if (!createIndexSQL.trim().endsWith(";")) {
      sql.append(";");
    }

    return sql.toString();
  }

  @Override
  public String toString() {
    return "PostgreSQLIndexDDL{" +
            "indexName='" + indexName + '\'' +
            ", originalIndexName='" + originalIndexName + '\'' +
            ", tableName='" + tableName + '\'' +
            ", schemaName='" + schemaName + '\'' +
            ", isSupported=" + isSupported +
            ", executionPhase='" + executionPhase + '\'' +
            '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    PostgreSQLIndexDDL that = (PostgreSQLIndexDDL) obj;
    return originalIndexName.equals(that.originalIndexName) &&
            schemaName.equals(that.schemaName);
  }

  @Override
  public int hashCode() {
    return originalIndexName.hashCode() * 31 + (schemaName != null ? schemaName.hashCode() : 0);
  }
}