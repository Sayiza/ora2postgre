package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.ForLoopStatement;
import me.christianrobert.ora2postgre.plsql.ast.LoopStatement;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.SelectStatement;
import me.christianrobert.ora2postgre.plsql.ast.Statement;
import me.christianrobert.ora2postgre.plsql.ast.WhileLoopStatement;

import java.util.ArrayList;
import java.util.List;

public class VisitLoopStatement {
  public static PlSqlAst visit(
          PlSqlParser.Loop_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    List<Statement> statements = new ArrayList<>();

    // Handle FOR loops
    if (ctx.FOR() != null) {
      if (ctx.cursor_loop_param().record_name() != null
              && ctx.cursor_loop_param().select_statement() != null) {
        if (ctx.seq_of_statements() != null
                && ctx.seq_of_statements().statement() != null) {
          for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
            statements.add((Statement) astBuilder.visit(stmt));
          }
        }
        String nameRef = ctx.cursor_loop_param().record_name().getText();
        ctx.cursor_loop_param().record_name().getText();
        SelectStatement sel = (SelectStatement) astBuilder.visit(ctx.cursor_loop_param().select_statement());
        return new ForLoopStatement(
                astBuilder.getSchema(),
                nameRef,
                sel,
                statements
        );
      }
    }

    // Handle WHILE loops
    if (ctx.WHILE() != null) {
      // Parse WHILE condition
      Expression condition = (Expression) astBuilder.visit(ctx.condition());

      // Parse loop body statements
      if (ctx.seq_of_statements() != null && ctx.seq_of_statements().statement() != null) {
        for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
          Statement statement = (Statement) astBuilder.visit(stmt);
          if (statement != null) {
            statements.add(statement);
          }
        }
      }

      return new WhileLoopStatement(condition, statements);
    }

    // Handle plain LOOP...END LOOP (no WHILE or FOR)
    // Parse loop body statements
    if (ctx.seq_of_statements() != null && ctx.seq_of_statements().statement() != null) {
      for (PlSqlParser.StatementContext stmt : ctx.seq_of_statements().statement()) {
        Statement statement = (Statement) astBuilder.visit(stmt);
        if (statement != null) {
          statements.add(statement);
        }
      }
    }

    return new LoopStatement(statements);
  }
}
