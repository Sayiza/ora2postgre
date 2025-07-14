package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * Represents a Common Table Expression (CTE) in Oracle WITH clause.
 * Oracle: WITH cte_name (col1, col2) AS (SELECT ...) 
 * PostgreSQL: WITH cte_name (col1, col2) AS (SELECT ...)
 */
public class CommonTableExpression extends PlSqlAst {
  
  private String queryName;
  private List<String> columnList; // Optional column list
  private SelectSubQuery subQuery;
  private boolean recursive; // For recursive CTEs
  
  public CommonTableExpression(String queryName, List<String> columnList, SelectSubQuery subQuery, boolean recursive) {
    this.queryName = queryName;
    this.columnList = columnList;
    this.subQuery = subQuery;
    this.recursive = recursive;
  }
  
  public String getQueryName() {
    return queryName;
  }
  
  public List<String> getColumnList() {
    return columnList;
  }
  
  public SelectSubQuery getSubQuery() {
    return subQuery;
  }
  
  public boolean isRecursive() {
    return recursive;
  }
  
  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "CommonTableExpression{" +
           "queryName='" + queryName + '\'' +
           ", columnList=" + columnList +
           ", recursive=" + recursive +
           '}';
  }
  
  /**
   * Transforms Oracle CTE to PostgreSQL CTE.
   * Most Oracle WITH clause patterns are directly compatible with PostgreSQL.
   */
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // Add CTE name
    b.append(queryName);
    
    // Add optional column list
    if (columnList != null && !columnList.isEmpty()) {
      b.append(" (");
      for (int i = 0; i < columnList.size(); i++) {
        if (i > 0) b.append(", ");
        b.append(columnList.get(i));
      }
      b.append(")");
    }
    
    // Add AS clause with subquery
    b.append(" AS (");
    if (subQuery != null) {
      b.append(subQuery.toPostgre(data));
    }
    b.append(")");
    
    return b.toString();
  }
}