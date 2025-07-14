package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class representing an ORDER BY element in SQL queries.
 * Handles column expression and sort direction (ASC/DESC).
 */
public class OrderByElement extends PlSqlAst {
  
  public enum SortDirection {
    ASC, DESC
  }
  
  private Expression expression;
  private SortDirection direction;
  private boolean nullsFirst;
  private boolean nullsLast;

  public OrderByElement() {
    // Default constructor
  }

  public OrderByElement(Expression expression, SortDirection direction) {
    this.expression = expression;
    this.direction = direction;
  }

  public OrderByElement(Expression expression, SortDirection direction, boolean nullsFirst, boolean nullsLast) {
    this.expression = expression;
    this.direction = direction;
    this.nullsFirst = nullsFirst;
    this.nullsLast = nullsLast;
  }

  public String toPostgre(Everything data) {
    StringBuilder result = new StringBuilder();
    
    if (expression != null) {
      result.append(expression.toPostgre(data));
    }
    
    if (direction != null) {
      result.append(" ").append(direction.toString());
    }
    
    if (nullsFirst) {
      result.append(" NULLS FIRST");
    } else if (nullsLast) {
      result.append(" NULLS LAST");
    }
    
    return result.toString();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  // Getters and setters
  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }

  public SortDirection getDirection() {
    return direction;
  }

  public void setDirection(SortDirection direction) {
    this.direction = direction;
  }

  public boolean isNullsFirst() {
    return nullsFirst;
  }

  public void setNullsFirst(boolean nullsFirst) {
    this.nullsFirst = nullsFirst;
  }

  public boolean isNullsLast() {
    return nullsLast;
  }

  public void setNullsLast(boolean nullsLast) {
    this.nullsLast = nullsLast;
  }

  @Override
  public String toString() {
    return "OrderByElement{" +
        "expression=" + expression +
        ", direction=" + direction +
        ", nullsFirst=" + nullsFirst +
        ", nullsLast=" + nullsLast +
        '}';
  }
}