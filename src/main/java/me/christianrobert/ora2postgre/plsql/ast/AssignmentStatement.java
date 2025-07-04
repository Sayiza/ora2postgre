package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

public class AssignmentStatement extends Statement {
  private final String target; // e.g., "vVariable"
  private final Expression expression; // e.g., Expression{rawText="0"}

  public AssignmentStatement(String target, Expression expression) {
    this.target = target;
    this.expression = expression;
  }

  public String getTarget() { return target; }
  public Expression getExpression() { return expression; }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "AssignmentStatement{target=" + target + ", expression=" + expression + "}";
  }

  // toJava() method removed - assignments stay in PostgreSQL

  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();
    b.append(data.getIntendation())
            .append(target)
            .append(" := ")
            .append(expression.toPostgre(data));
    return b.toString();
  }
}