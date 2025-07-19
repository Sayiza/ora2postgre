package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.*;

public class VisitExitStatement {
  public static PlSqlAst visit(
          PlSqlParser.Exit_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    String labelName = null;
    Expression condition = null;
    
    // Check for optional label name
    if (ctx.label_name() != null) {
      labelName = ctx.label_name().getText();
    }
    
    // Check for optional WHEN condition
    if (ctx.condition() != null) {
      condition = (Expression) astBuilder.visit(ctx.condition());
    }
    
    return new ExitStatement(labelName, condition);
  }
}