package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

/**
 * AST class representing CLOSE cursor statements.
 * Oracle: CLOSE cursor_name;
 * PostgreSQL: Same syntax
 */
public class CloseStatement extends Statement {
  private final String cursorName;

  public CloseStatement(String cursorName) {
    this.cursorName = cursorName;
  }

  public String getCursorName() {
    return cursorName;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "CloseStatement{" +
            "cursor='" + cursorName + '\'' +
            '}';
  }

  /**
   * Generate PostgreSQL CLOSE statement.
   * PostgreSQL syntax is identical to Oracle for CLOSE statements.
   */
  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    b.append("CLOSE ").append(cursorName).append(";");
    
    return b.toString();
  }
}