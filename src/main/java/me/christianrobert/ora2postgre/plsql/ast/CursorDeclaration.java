package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * AST class representing cursor declarations.
 * Oracle: CURSOR cursor_name [(parameter_list)] [RETURN type] IS select_statement;
 * PostgreSQL: cursor_name CURSOR [(parameter_list)] FOR select_statement;
 */
public class CursorDeclaration extends PlSqlAst {
  private final String cursorName;
  private final List<Parameter> parameters; // Optional cursor parameters
  private final String returnType; // Optional RETURN type
  private final SelectStatement selectStatement; // The SELECT query for the cursor

  public CursorDeclaration(String cursorName, List<Parameter> parameters, String returnType, SelectStatement selectStatement) {
    this.cursorName = cursorName;
    this.parameters = parameters;
    this.returnType = returnType;
    this.selectStatement = selectStatement;
  }

  public String getCursorName() {
    return cursorName;
  }

  public List<Parameter> getParameters() {
    return parameters;
  }

  public String getReturnType() {
    return returnType;
  }

  public SelectStatement getSelectStatement() {
    return selectStatement;
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }

  public boolean hasReturnType() {
    return returnType != null && !returnType.trim().isEmpty();
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "CursorDeclaration{" +
            "name='" + cursorName + '\'' +
            ", hasParameters=" + hasParameters() +
            ", hasReturnType=" + hasReturnType() +
            '}';
  }

  /**
   * Generate PostgreSQL cursor declaration.
   * PostgreSQL syntax: cursor_name CURSOR [(parameter_list)] FOR select_statement;
   */
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // PostgreSQL: cursor_name CURSOR FOR SELECT...
    b.append(data.getIntendation()).append(cursorName).append(" CURSOR");
    
    // Add parameters if present
    if (hasParameters()) {
      b.append("(");
      for (int i = 0; i < parameters.size(); i++) {
        if (i > 0) {
          b.append(", ");
        }
        Parameter param = parameters.get(i);
        // PostgreSQL cursor parameters: name type
        b.append(param.getName()).append(" ").append(param.getDataType().toPostgre(data));
      }
      b.append(")");
    }
    
    // PostgreSQL uses FOR instead of IS (RETURN type is not used)
    b.append(" FOR ");
    if (selectStatement != null) {
      b.append(selectStatement.toPostgre(data));
    } else {
      b.append("/* TODO: cursor SELECT statement */");
    }
    
    b.append(";");
    
    return b.toString();
  }
}