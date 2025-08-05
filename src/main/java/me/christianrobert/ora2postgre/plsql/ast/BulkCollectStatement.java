package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * Represents an Oracle BULK COLLECT INTO statement that loads multiple rows into arrays.
 * 
 * Oracle: SELECT column1, column2 BULK COLLECT INTO array_var1, array_var2 FROM table_name WHERE condition;
 * PostgreSQL: array_var1 := ARRAY(SELECT column1 FROM table_name WHERE condition);
 *             array_var2 := ARRAY(SELECT column2 FROM table_name WHERE condition);
 */
public class BulkCollectStatement extends Statement {
  private final List<String> selectedColumns; // SELECT column list
  private final List<String> intoArrays; // BULK COLLECT INTO array variable list
  private final String tableName;
  private final String schemaName;
  private final Expression whereClause; // Optional WHERE condition

  public BulkCollectStatement(List<String> selectedColumns, List<String> intoArrays, 
                            String schemaName, String tableName, Expression whereClause) {
    this.selectedColumns = selectedColumns;
    this.intoArrays = intoArrays;
    this.schemaName = schemaName;
    this.tableName = tableName;
    this.whereClause = whereClause;
  }

  // Constructor without WHERE clause
  public BulkCollectStatement(List<String> selectedColumns, List<String> intoArrays,
                            String schemaName, String tableName) {
    this(selectedColumns, intoArrays, schemaName, tableName, null);
  }

  public List<String> getSelectedColumns() {
    return selectedColumns;
  }

  public List<String> getIntoArrays() {
    return intoArrays;
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
    return "BulkCollectStatement{" +
            "table=" + (schemaName != null ? schemaName + "." : "") + tableName +
            ", columns=" + (selectedColumns != null ? selectedColumns.size() : 0) +
            ", arrays=" + (intoArrays != null ? intoArrays.size() : 0) +
            ", hasWhere=" + (whereClause != null) + "}";
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    // Transform BULK COLLECT by generating separate assignment statements for each array
    // Oracle: SELECT col1, col2 BULK COLLECT INTO arr1, arr2 FROM table WHERE condition;
    // PostgreSQL: arr1 := ARRAY(SELECT col1 FROM table WHERE condition);
    //             arr2 := ARRAY(SELECT col2 FROM table WHERE condition);

    if (selectedColumns != null && intoArrays != null && 
        selectedColumns.size() == intoArrays.size()) {
      
      for (int i = 0; i < selectedColumns.size(); i++) {
        if (i > 0) {
          b.append("\n");
        }
        
        String column = selectedColumns.get(i);
        String arrayVar = intoArrays.get(i);
        
        b.append(arrayVar).append(" := ARRAY(");
        b.append("SELECT ").append(column.toLowerCase());
        
        // Handle FROM clause with schema resolution
        b.append(" FROM ");
        String resolvedSchema = resolveSchemaName(data);
        if (resolvedSchema != null && !resolvedSchema.isEmpty()) {
          b.append(resolvedSchema.toUpperCase()).append(".");
        }
        b.append(tableName.toUpperCase());
        
        // Handle WHERE clause
        if (hasWhereClause()) {
          b.append(" WHERE ");
          b.append(whereClause.toPostgre(data));
        }
        
        b.append(");");
      }
    } else if (selectedColumns != null && selectedColumns.size() == 1 && 
               selectedColumns.get(0).equals("*") && intoArrays != null && intoArrays.size() == 1) {
      // Special case: SELECT * BULK COLLECT INTO single_array
      // This is a more complex scenario - for now, generate a comment
      b.append("-- BULK COLLECT with SELECT * requires manual conversion");
      b.append("\n").append("-- ").append(intoArrays.get(0));
      b.append(" := ARRAY(SELECT ROW(*) FROM ");
      
      String resolvedSchema = resolveSchemaName(data);
      if (resolvedSchema != null && !resolvedSchema.isEmpty()) {
        b.append(resolvedSchema.toUpperCase()).append(".");
      }
      b.append(tableName.toUpperCase());
      
      if (hasWhereClause()) {
        b.append(" WHERE ");
        b.append(whereClause.toPostgre(data));
      }
      b.append(");");
    } else {
      // Column count mismatch or other issues - generate comment
      b.append("-- BULK COLLECT: Column/array count mismatch - requires manual conversion");
      b.append("\n").append("-- Columns: ").append(selectedColumns);
      b.append("\n").append("-- Arrays: ").append(intoArrays);
    }

    return b.toString();
  }

  /**
   * Resolves the schema name using the same logic as SelectIntoStatement
   */
  private String resolveSchemaName(Everything data) {
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

    return resolvedSchema;
  }

  /**
   * Gets the current schema context for resolving unqualified table names.
   * This should be the schema where the function/procedure containing this BULK COLLECT is defined.
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