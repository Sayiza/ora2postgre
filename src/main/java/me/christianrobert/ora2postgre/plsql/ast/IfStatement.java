package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

public class IfStatement extends Statement {
  private final Expression condition;
  private final List<Statement> thenStatements;
  private final List<ElsifPart> elsifParts;
  private final List<Statement> elseStatements;

  public IfStatement(Expression condition, List<Statement> thenStatements,
                     List<ElsifPart> elsifParts, List<Statement> elseStatements) {
    this.condition = condition;
    this.thenStatements = thenStatements;
    this.elsifParts = elsifParts;
    this.elseStatements = elseStatements;
  }

  public Expression getCondition() {
    return condition;
  }

  public List<Statement> getThenStatements() {
    return thenStatements;
  }

  public List<ElsifPart> getElsifParts() {
    return elsifParts;
  }

  public List<Statement> getElseStatements() {
    return elseStatements;
  }

  @Override
  public <T> T accept(PlSqlAstVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "IfStatement{condition=" + condition + ", thenStatements=" + thenStatements.size() +
            ", elsifParts=" + (elsifParts != null ? elsifParts.size() : 0) +
            ", elseStatements=" + (elseStatements != null ? elseStatements.size() : 0) + "}";
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    // IF condition THEN
    b.append(data.getIntendation()).append("IF ").append(condition.toPostgre(data)).append(" THEN\n");

    // THEN statements
    data.intendMore();
    for (Statement stmt : thenStatements) {
      b.append(stmt.toPostgre(data)).append("\n");
    }
    data.intendLess();

    // ELSIF parts
    if (elsifParts != null) {
      for (ElsifPart elsif : elsifParts) {
        b.append(data.getIntendation()).append("ELSIF ").append(elsif.getCondition().toPostgre(data)).append(" THEN\n");
        data.intendMore();
        for (Statement stmt : elsif.getStatements()) {
          b.append(stmt.toPostgre(data)).append("\n");
        }
        data.intendLess();
      }
    }

    // ELSE part
    if (elseStatements != null && !elseStatements.isEmpty()) {
      b.append(data.getIntendation()).append("ELSE\n");
      data.intendMore();
      for (Statement stmt : elseStatements) {
        b.append(stmt.toPostgre(data)).append("\n");
      }
      data.intendLess();
    }

    // END IF
    b.append(data.getIntendation()).append("END IF;");

    return b.toString();
  }

  /**
   * Represents an ELSIF part of an IF statement
   */
  public static class ElsifPart {
    private final Expression condition;
    private final List<Statement> statements;

    public ElsifPart(Expression condition, List<Statement> statements) {
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
    public String toString() {
      return "ElsifPart{condition=" + condition + ", statements=" + statements.size() + "}";
    }
  }
}