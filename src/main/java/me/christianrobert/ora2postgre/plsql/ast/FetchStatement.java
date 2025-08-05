package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * AST class representing FETCH cursor statements.
 * Oracle: FETCH cursor_name INTO variable1, variable2, ...;
 * PostgreSQL: Same syntax
 */
public class FetchStatement extends Statement {
  private final String cursorName;
  private final List<String> intoVariables; // Variables to fetch into

  public FetchStatement(String cursorName, List<String> intoVariables) {
    this.cursorName = cursorName;
    this.intoVariables = intoVariables;
  }

  public String getCursorName() {
    return cursorName;
  }

  public List<String> getIntoVariables() {
    return intoVariables;
  }

  public boolean hasIntoVariables() {
    return intoVariables != null && !intoVariables.isEmpty();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "FetchStatement{" +
            "cursor='" + cursorName + '\'' +
            ", variables=" + (intoVariables != null ? intoVariables.size() : 0) +
            '}';
  }

  /**
   * Generate PostgreSQL FETCH statement.
   * PostgreSQL syntax is identical to Oracle for FETCH statements.
   */
  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    b.append("FETCH ").append(cursorName);
    
    // Add INTO clause if variables are present
    if (hasIntoVariables()) {
      b.append(" INTO ");
      for (int i = 0; i < intoVariables.size(); i++) {
        if (i > 0) {
          b.append(", ");
        }
        b.append(intoVariables.get(i));
      }
    }
    
    b.append(";");
    
    return b.toString();
  }
}