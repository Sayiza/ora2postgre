package me.christianrobert.ora2postgre.oracledb;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.oracledb.tools.CodeCleaner;
import me.christianrobert.ora2postgre.plsql.ast.tools.TypeConverter;

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
   * @return List of SQL statements
   */
  public List<String> toPostgre(Everything data) {
    List<String> statements = new ArrayList<>();

    // Build CREATE TABLE statement
    StringBuilder createTable = new StringBuilder("CREATE TABLE ");
    createTable.append(schema)
            .append(".")
            .append(tableName).append(" (\n");

    // Columns
    List<String> columnDefs = new ArrayList<>();
    for (ColumnMetadata col : columns) {
      columnDefs.add(col.toPostgre(data, this.schema, this.tableName));
    }
    createTable.append(String.join(",\n", columnDefs));
    createTable.append("\n);");
    statements.add(createTable.toString());

    return statements;
  }

  public List<String>  toPostgreConstrain() {
    List<String> statements = new ArrayList<>();

    // Primary key constraints as ALTER TABLE
    for (ConstraintMetadata cons : constraints) {
      if ("P".equals(cons.getConstraintType())) {
        StringBuilder alterTable = new StringBuilder("ALTER TABLE ");
        alterTable.append(tableName);
        alterTable.append(" ADD CONSTRAINT ");
        alterTable.append(cons.getConstraintName());
        alterTable.append(" PRIMARY KEY (");
        alterTable.append(String.join(", ", cons.getColumnNames()));
        alterTable.append(");");
        statements.add(alterTable.toString());
      }
    }

    return statements;
  }
}