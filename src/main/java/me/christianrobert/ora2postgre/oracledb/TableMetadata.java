package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.plsql.ast.tools.managers.TableTransformationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableMetadata {
  private String schema; // Oracle schema (user)
  private String tableName;
  private List<ColumnMetadata> columns;
  private List<ConstraintMetadata> constraints;

  // TODO use new map
  // Common Oracle to PostgreSQL data type mapping
  private static final Map<String, String> ORACLE_TO_POSTGRES_TYPES = Map.ofEntries(
          Map.entry("VARCHAR2", "VARCHAR"),
          Map.entry("NVARCHAR2", "VARCHAR"),
          Map.entry("CHAR", "CHAR"),
          Map.entry("NCHAR", "CHAR"),
          Map.entry("NUMBER", "NUMERIC"), // Will refine based on precision/scale
          Map.entry("INTEGER", "INTEGER"),
          Map.entry("FLOAT", "DOUBLE PRECISION"),
          Map.entry("DATE", "TIMESTAMP"),
          Map.entry("TIMESTAMP", "TIMESTAMP"),
          Map.entry("CLOB", "TEXT"),
          Map.entry("BLOB", "BYTEA"),
          Map.entry("RAW", "BYTEA")
  );

  public TableMetadata(String schema, String tableName) {
    this.schema = schema;
    this.tableName = tableName;
    this.columns = new ArrayList<>();
    this.constraints = new ArrayList<>();
  }

  // Getters and setters
  public String getSchema() { return schema; }
  public String getTableName() { return tableName; }
  public List<ColumnMetadata> getColumns() { return columns; }
  public List<ConstraintMetadata> getConstraints() { return constraints; }

  public void addColumn(ColumnMetadata column) { columns.add(column); }
  public void addConstraint(ConstraintMetadata constraint) { constraints.add(constraint); }

  @Override
  public String toString() {
    return "TableMetadata{schema='" + schema + "', tableName='" + tableName + "', columns=" + columns.size() + ", constraints=" + constraints.size() + "}";
  }

  public String toJava(String javaPackageName) {
    return "TODO entity";
  }

  /**
   * Generates PostgreSQL-compatible CREATE TABLE and ALTER TABLE statements for the table and its constraints.
   * @deprecated Use TableTransformationManager instead for better strategy-based transformation
   * @return List of SQL statements
   */
  @Deprecated
  public List<String> toPostgre(Everything data) {
    // Delegate to strategy manager for consistent transformation
    TableTransformationManager manager = new TableTransformationManager();
    return manager.transform(this, data);
  }
}