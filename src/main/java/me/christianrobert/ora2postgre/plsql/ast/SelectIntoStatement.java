package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class SelectIntoStatement extends Statement {
  private final List<String> selectedColumns; // SELECT column list
  private final List<String> intoVariables; // INTO variable list
  private final String tableName;
  private final String schemaName;
  private final Expression whereClause; // Optional WHERE condition

  public SelectIntoStatement(List<String> selectedColumns, List<String> intoVariables, 
                            String schemaName, String tableName, Expression whereClause) {
    this.selectedColumns = selectedColumns;
    this.intoVariables = intoVariables;
    this.schemaName = schemaName;
    this.tableName = tableName;
    this.whereClause = whereClause;
  }

  // Constructor without WHERE clause
  public SelectIntoStatement(List<String> selectedColumns, List<String> intoVariables,
                            String schemaName, String tableName) {
    this(selectedColumns, intoVariables, schemaName, tableName, null);
  }

  public List<String> getSelectedColumns() {
    return selectedColumns;
  }

  public List<String> getIntoVariables() {
    return intoVariables;
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
    return "SelectIntoStatement{" +
            "table=" + (schemaName != null ? schemaName + "." : "") + tableName +
            ", columns=" + (selectedColumns != null ? selectedColumns.size() : 0) +
            ", variables=" + (intoVariables != null ? intoVariables.size() : 0) +
            ", hasWhere=" + (whereClause != null) + "}";
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    b.append(data.getIntendation()).append("SELECT ");

    // Handle SELECT column list
    if (selectedColumns != null && !selectedColumns.isEmpty()) {
      for (int i = 0; i < selectedColumns.size(); i++) {
        if (i > 0) {
          b.append(", ");
        }
        b.append(selectedColumns.get(i).toLowerCase());
      }
    } else {
      b.append("*"); // Default to all columns if none specified
    }

    // Handle INTO clause
    if (intoVariables != null && !intoVariables.isEmpty()) {
      b.append(" INTO ");
      for (int i = 0; i < intoVariables.size(); i++) {
        if (i > 0) {
          b.append(", ");
        }
        b.append(intoVariables.get(i));
      }
    }

    // Handle FROM clause with schema resolution (same pattern as DML statements)
    b.append(" FROM ");
    
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
   * This should be the schema where the function/procedure containing this SELECT INTO is defined.
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