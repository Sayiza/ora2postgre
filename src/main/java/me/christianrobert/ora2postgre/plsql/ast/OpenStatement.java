package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * AST class representing OPEN cursor statements.
 * Oracle: OPEN cursor_name [(parameter_values)];
 * PostgreSQL: Same syntax
 */
public class OpenStatement extends Statement {
  private final String cursorName;
  private final List<Expression> parameters; // Optional parameter values

  public OpenStatement(String cursorName, List<Expression> parameters) {
    this.cursorName = cursorName;
    this.parameters = parameters;
  }

  public String getCursorName() {
    return cursorName;
  }

  public List<Expression> getParameters() {
    return parameters;
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "OpenStatement{" +
            "cursor='" + cursorName + '\'' +
            ", hasParameters=" + hasParameters() +
            '}';
  }

  /**
   * Generate PostgreSQL OPEN statement.
   * PostgreSQL syntax is identical to Oracle for OPEN statements.
   */
  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    b.append(data.getIntendation()).append("OPEN ").append(cursorName);
    
    // Add parameter values if present
    if (hasParameters()) {
      b.append("(");
      for (int i = 0; i < parameters.size(); i++) {
        if (i > 0) {
          b.append(", ");
        }
        b.append(parameters.get(i).toPostgre(data));
      }
      b.append(")");
    }
    
    b.append(";");
    
    return b.toString();
  }
}