package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class UpdateStatement extends Statement {
  private final String tableName;
  private final String schemaName;
  private final List<UpdateSetClause> setColumns; // SET column = value pairs
  private final Expression whereClause; // Optional WHERE condition

  public UpdateStatement(String schemaName, String tableName, List<UpdateSetClause> setColumns, Expression whereClause) {
    this.schemaName = schemaName;
    this.tableName = tableName;
    this.setColumns = setColumns;
    this.whereClause = whereClause;
  }

  // Constructor without WHERE clause
  public UpdateStatement(String schemaName, String tableName, List<UpdateSetClause> setColumns) {
    this(schemaName, tableName, setColumns, null);
  }

  public String getTableName() {
    return tableName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public List<UpdateSetClause> getSetColumns() {
    return setColumns;
  }

  public Expression getWhereClause() {
    return whereClause;
  }

  public boolean hasWhereClause() {
    return whereClause != null;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "UpdateStatement{" +
            "table=" + (schemaName != null ? schemaName + "." : "") + tableName +
            ", setClauses=" + (setColumns != null ? setColumns.size() : 0) +
            ", hasWhere=" + (whereClause != null) + "}";
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    b.append("UPDATE ");

    // Resolve schema using Everything's schema resolution logic (same as INSERT)
    String resolvedSchema = null;

    if (schemaName != null && !schemaName.isEmpty()) {
      // Schema was explicitly provided in Oracle code (e.g., SCHEMA.TABLE)
      try {
        resolvedSchema = data.lookupSchema4Field(tableName, schemaName);
      } catch (Exception e) {
        // If schema resolution fails, use the provided schema as-is
        resolvedSchema = schemaName;
      }
    } else {
      // No schema prefix in Oracle code - table is in current schema or is a synonym
      // Use the current schema context from the function/procedure
      String currentSchema = getCurrentSchema(data);
      if (currentSchema != null) {
        try {
          resolvedSchema = data.lookupSchema4Field(tableName, currentSchema);
        } catch (Exception e) {
          // If synonym/table lookup fails, assume it's in the current schema
          resolvedSchema = currentSchema;
        }
      }
    }

    // Always emit schema prefix for PostgreSQL reliability
    if (resolvedSchema != null && !resolvedSchema.isEmpty()) {
      b.append(resolvedSchema.toUpperCase()).append(".");
    }
    b.append(tableName.toUpperCase());

    // Handle SET clause
    if (setColumns != null && !setColumns.isEmpty()) {
      b.append(" SET ");
      for (int i = 0; i < setColumns.size(); i++) {
        if (i > 0) {
          b.append(", ");
        }
        b.append(setColumns.get(i).toPostgre(data));
      }
    }

    // Handle WHERE clause
    if (hasWhereClause()) {
      b.append(" WHERE ");
      b.append(whereClause.toPostgre(data));
    }

    b.append(";");

    return b.toString();
  }

  /**
   * Gets the current schema context for resolving unqualified table names.
   * This should be the schema where the function/procedure containing this UPDATE is defined.
   */
  private String getCurrentSchema(Everything data) {
    // For now, use the first user schema as the current schema context
    // TODO: This could be enhanced to use actual function/procedure schema context
    if (!data.getUserNames().isEmpty()) {
      return data.getUserNames().get(0);
    }
    return null;
  }

  /**
   * Represents a SET column = value clause in an UPDATE statement
   */
  public static class UpdateSetClause {
    private final String columnName;
    private final Expression value;

    public UpdateSetClause(String columnName, Expression value) {
      this.columnName = columnName;
      this.value = value;
    }

    public String getColumnName() {
      return columnName;
    }

    public Expression getValue() {
      return value;
    }

    public String toPostgre(Everything data) {
      return columnName.toLowerCase() + " = " + value.toPostgre(data);
    }

    @Override
    public String toString() {
      return columnName + " = " + value;
    }
  }
}