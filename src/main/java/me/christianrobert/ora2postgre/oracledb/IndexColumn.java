package me.christianrobert.ora2postgre.oracledb;

/**
 * Represents a column in an Oracle database index.
 * Handles both regular columns and functional expressions.
 */
public class IndexColumn {
  private final String columnName;
  private final String columnExpression; // For functional indexes
  private final String sortOrder; // ASC, DESC
  private final int position; // Column position in composite index
  private final boolean descending;

  // Constructor for regular column
  public IndexColumn(String columnName, String sortOrder, int position, boolean descending) {
    this.columnName = columnName;
    this.columnExpression = null;
    this.sortOrder = sortOrder;
    this.position = position;
    this.descending = descending;
  }

  // Constructor for functional index column
  public IndexColumn(String columnName, String columnExpression, String sortOrder, int position, boolean descending) {
    this.columnName = columnName;
    this.columnExpression = columnExpression;
    this.sortOrder = sortOrder;
    this.position = position;
    this.descending = descending;
  }

  // Getters
  public String getColumnName() {
    return columnName;
  }

  public String getColumnExpression() {
    return columnExpression;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public int getPosition() {
    return position;
  }

  public boolean isDescending() {
    return descending;
  }

  // Utility methods
  public boolean isFunctional() {
    return columnExpression != null && !columnExpression.trim().isEmpty();
  }

  public boolean isAscending() {
    return !descending;
  }

  /**
   * Gets the effective column reference for use in DDL generation.
   * For functional indexes, returns the expression; for regular columns, returns the column name.
   */
  public String getEffectiveColumnReference() {
    if (isFunctional()) {
      return columnExpression;
    }
    return columnName;
  }

  /**
   * Determines if this column has a complex expression that may need manual review.
   */
  public boolean hasComplexExpression() {
    if (!isFunctional()) {
      return false;
    }

    // Check for Oracle-specific functions that might need transformation
    String expr = columnExpression.toUpperCase();
    return expr.contains("TO_CHAR") || expr.contains("TO_DATE") ||
            expr.contains("SUBSTR") || expr.contains("DECODE") ||
            expr.contains("NVL") || expr.contains("CASE") ||
            expr.contains("EXTRACT") || expr.contains("TRUNC");
  }

  /**
   * Gets a simplified description of the column for reporting.
   */
  public String getDescription() {
    if (isFunctional()) {
      return "Expression: " + columnExpression + (descending ? " DESC" : " ASC");
    }
    return columnName + (descending ? " DESC" : " ASC");
  }

  @Override
  public String toString() {
    return "IndexColumn{" +
            "columnName='" + columnName + '\'' +
            ", columnExpression='" + columnExpression + '\'' +
            ", sortOrder='" + sortOrder + '\'' +
            ", position=" + position +
            ", descending=" + descending +
            '}';
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    IndexColumn that = (IndexColumn) obj;
    return position == that.position &&
            descending == that.descending &&
            columnName.equals(that.columnName) &&
            java.util.Objects.equals(columnExpression, that.columnExpression) &&
            java.util.Objects.equals(sortOrder, that.sortOrder);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(columnName, columnExpression, sortOrder, position, descending);
  }
}