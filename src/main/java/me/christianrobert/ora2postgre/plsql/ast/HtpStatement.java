package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class HtpStatement extends Statement {

  private Expression argument;

  public HtpStatement(Expression argument) {
    this.argument = argument;
  }
  
  // Legacy constructor for backward compatibility
  public HtpStatement(String what) {
    // Convert raw text to expression for backward compatibility
    this.argument = new Expression(new LogicalExpression(new UnaryLogicalExpression(what)));
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Htp{" + (argument != null ? argument.toString() : "null") + "}";
  }

  // toJava() method removed - HTP calls stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    
    // Transform the argument expression, which will handle package variables
    String transformedArgument = argument != null ? argument.toPostgre(data) : "''";
    
    b.append("CALL SYS.HTP_p(")
            .append(transformedArgument)
            .append(");\n");
    return b.toString();
  }

}