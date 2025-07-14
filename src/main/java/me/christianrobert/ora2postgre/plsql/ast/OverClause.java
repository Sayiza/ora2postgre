package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;
import java.util.List;

/**
 * AST class representing an Oracle OVER clause for analytical functions.
 * Handles PARTITION BY, ORDER BY, and windowing specifications.
 */
public class OverClause extends PlSqlAst {
  
  private List<Expression> partitionByColumns;
  private List<OrderByElement> orderByElements; 
  private WindowingClause windowingClause;

  public OverClause() {
    // Default constructor
  }

  public OverClause(List<Expression> partitionByColumns, List<OrderByElement> orderByElements, WindowingClause windowingClause) {
    this.partitionByColumns = partitionByColumns;
    this.orderByElements = orderByElements;
    this.windowingClause = windowingClause;
  }

  public String toPostgre(Everything data) {
    StringBuilder result = new StringBuilder("OVER (");
    
    // Add PARTITION BY clause if present
    if (partitionByColumns != null && !partitionByColumns.isEmpty()) {
      result.append("PARTITION BY ");
      for (int i = 0; i < partitionByColumns.size(); i++) {
        if (i > 0) result.append(", ");
        result.append(partitionByColumns.get(i).toPostgre(data));
      }
    }
    
    // Add ORDER BY clause if present
    if (orderByElements != null && !orderByElements.isEmpty()) {
      if (partitionByColumns != null && !partitionByColumns.isEmpty()) {
        result.append(" ");
      }
      result.append("ORDER BY ");
      for (int i = 0; i < orderByElements.size(); i++) {
        if (i > 0) result.append(", ");
        result.append(orderByElements.get(i).toPostgre(data));
      }
    }
    
    // Add windowing clause if present  
    if (windowingClause != null) {
      result.append(" ").append(windowingClause.toPostgre(data));
    }
    
    result.append(")");
    return result.toString();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  // Getters and setters
  public List<Expression> getPartitionByColumns() {
    return partitionByColumns;
  }

  public void setPartitionByColumns(List<Expression> partitionByColumns) {
    this.partitionByColumns = partitionByColumns;
  }

  public List<OrderByElement> getOrderByElements() {
    return orderByElements;
  }

  public void setOrderByElements(List<OrderByElement> orderByElements) {
    this.orderByElements = orderByElements;
  }

  public WindowingClause getWindowingClause() {
    return windowingClause;
  }

  public void setWindowingClause(WindowingClause windowingClause) {
    this.windowingClause = windowingClause;
  }

  @Override
  public String toString() {
    return "OverClause{" +
        "partitionByColumns=" + partitionByColumns +
        ", orderByElements=" + orderByElements +
        ", windowingClause=" + windowingClause +
        '}';
  }
}