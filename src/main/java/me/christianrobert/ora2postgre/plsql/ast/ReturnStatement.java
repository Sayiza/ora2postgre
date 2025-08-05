package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class ReturnStatement extends Statement {

  Expression expression;

  public ReturnStatement(Expression expression) {
    this.expression = expression;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "Return{" + expression + "}";
  }

  // toJava() method removed - return statements stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append("return ")
            .append(expression.toPostgre(data))
            .append(";\n");
    return b.toString();
  }
}