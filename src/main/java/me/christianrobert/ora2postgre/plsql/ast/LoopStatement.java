package me.christianrobert.ora2postgre.plsql.ast;

import me.christianrobert.ora2postgre.global.Everything;

import java.util.List;

/**
 * Represents a plain LOOP statement in PL/SQL
 * Oracle: LOOP ... END LOOP;
 * PostgreSQL: LOOP ... END LOOP;
 * 
 * This is distinct from ForLoopStatement (FOR...LOOP) and WhileLoopStatement (WHILE...LOOP).
 * Plain loops are infinite loops that rely on EXIT statements or exceptions to terminate.
 */
public class LoopStatement extends Statement {
  private final List<Statement> statements;

  public LoopStatement(List<Statement> statements) {
    this.statements = statements;
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
    return "LoopStatement{statements=" + statements.size() + "}";
  }

  @Override
  public String toPostgre(Everything data) {
    StringBuilder b = new StringBuilder();

    // LOOP
    b.append("LOOP\n");

    // Loop body statements
    for (Statement stmt : statements) {
      b.append(stmt.toPostgre(data)).append("\n");
    }

    // END LOOP
    b.append("END LOOP;");

    return b.toString();
  }
}