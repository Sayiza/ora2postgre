package me.christianrobert.ora2postgre.plsql.builderfncs;

import me.christianrobert.ora2postgre.antlr.PlSqlParser;
import me.christianrobert.ora2postgre.plsql.PlSqlAstBuilder;
import me.christianrobert.ora2postgre.plsql.ast.Expression;
import me.christianrobert.ora2postgre.plsql.ast.PlSqlAst;
import me.christianrobert.ora2postgre.plsql.ast.ReturnStatement;

public class VisitReturnStatement {
  public static PlSqlAst visit(
          PlSqlParser.Return_statementContext ctx,
          PlSqlAstBuilder astBuilder) {
    
    Expression expression = (Expression) astBuilder.visit(ctx.expression());
    if (expression != null) {
      return new ReturnStatement(expression);
    }
    return new ReturnStatement(null);
  }
}