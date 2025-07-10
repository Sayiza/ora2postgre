package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * Represents a WHILE loop statement in PL/SQL
 * Oracle: WHILE condition LOOP ... END LOOP;
 * PostgreSQL: WHILE condition LOOP ... END LOOP;
 */
public class WhileLoopStatement extends Statement {
  private final Expression condition;
  private final List<Statement> statements;

  public WhileLoopStatement(Expression condition, List<Statement> statements) {
    this.condition = condition;
    this.statements = statements;
  }

  public Expression getCondition() {
    return condition;
  }

  public List<Statement> getStatements() {
    return statements;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "WhileLoopStatement{condition=" + condition + ", statements=" + statements.size() + "}";
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    // WHILE condition LOOP
    b.append(data.getIntendation()).append("WHILE ").append(condition.toPostgre(data)).append(" LOOP\n");

    // Loop body statements
    data.intendMore();
    for (Statement stmt : statements) {
      b.append(stmt.toPostgre(data)).append("\n");
    }
    data.intendLess();

    // END LOOP
    b.append(data.getIntendation()).append("END LOOP;");

    return b.toString();
  }
}