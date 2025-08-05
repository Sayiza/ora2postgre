package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import me.christianrobert.ora2postgre.global.SchemaResolutionUtils;

public class DeleteStatement extends Statement {
  private final String tableName;
  private final String schemaName;
  private final Expression whereClause; // Optional WHERE condition

  public DeleteStatement(String schemaName, String tableName, Expression whereClause) {
    this.schemaName = schemaName;
    this.tableName = tableName;
    this.whereClause = whereClause;
  }

  // Constructor without WHERE clause
  public DeleteStatement(String schemaName, String tableName) {
    this(schemaName, tableName, null);
  }

  public String getTableName() {
    return tableName;
  }

  public String getSchemaName() {
    return schemaName;
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
    return "DeleteStatement{" +
            "table=" + (schemaName != null ? schemaName + "." : "") + tableName +
            ", hasWhere=" + (whereClause != null) + "}";
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    b.append("DELETE FROM ");

    // Resolve schema using Everything's schema resolution logic (same as INSERT/UPDATE)
    String resolvedSchema = null;

    if (schemaName != null && !schemaName.isEmpty()) {
      // Schema was explicitly provided in Oracle code (e.g., SCHEMA.TABLE)
      try {
        resolvedSchema = SchemaResolutionUtils.lookupSchema4Field(data, tableName, schemaName);
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
          resolvedSchema = SchemaResolutionUtils.lookupSchema4Field(data, tableName, currentSchema);
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
   * This should be the schema where the function/procedure containing this DELETE is defined.
   */
  private String getCurrentSchema(Everything data) {
    // For now, use the first user schema as the current schema context
    // TODO: This could be enhanced to use actual function/procedure schema context
    if (!data.getUserNames().isEmpty()) {
      return data.getUserNames().get(0);
    }
    return null;
  }
}