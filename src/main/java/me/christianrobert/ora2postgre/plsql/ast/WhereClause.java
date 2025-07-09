package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * Represents a WHERE clause in SQL statements.
 * Supports two variants:
 * 1. CURRENT OF cursor_name
 * 2. condition (general expressions)
 */
public class WhereClause extends PlSqlAst {
  private final String cursorName;  // For "CURRENT OF cursor_name"
  private final Expression condition;  // For general conditions

  /**
   * Constructor for CURRENT OF cursor_name variant
   */
  public WhereClause(String cursorName) {
    this.cursorName = cursorName;
    this.condition = null;
  }

  /**
   * Constructor for condition variant
   */
  public WhereClause(Expression condition) {
    this.cursorName = null;
    this.condition = condition;
  }

  public boolean isCursorBased() {
    return cursorName != null;
  }

  public String getCursorName() {
    return cursorName;
  }

  public Expression getCondition() {
    return condition;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    if (isCursorBased()) {
      return "WHERE CURRENT OF " + cursorName;
    } else {
      return "WHERE " + (condition != null ? condition.toString() : "null");
    }
  }

  public String toPostgre(Everything data) {
    if (isCursorBased()) {
      return "WHERE CURRENT OF " + cursorName;
    } else if (condition != null) {
      return "WHERE " + condition.toPostgre(data);
    } else {
      return "";
    }
  }
}